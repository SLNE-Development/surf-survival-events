package dev.slne.surf.survival.events.freebuild.certificate

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.freebuild.plugin
import io.papermc.paper.datacomponent.DataComponentTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapView
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.logging.Level

/** A participant whose name is known, so a certificate can be drawn for them. */
data class NamedParticipant(val uuid: UUID, val name: String, val seconds: Long)

/** A newly issued certificate; [skinUnavailable] tells that it shows the fallback face instead of the skin. */
data class IssuedCertificate(val record: CertificateRecord, val skinUnavailable: Boolean)

data class BulkIssueResult(
    val issued: List<CertificateRecord>,
    val skipped: List<NamedParticipant>,
    val failed: List<NamedParticipant>,
    val withoutSkin: List<NamedParticipant>,
)

/** Outcome of [CertificateService.give]. */
sealed interface GiveResult {
    data class Issued(val certificate: IssuedCertificate) : GiveResult

    /** The player already had the certificate; it can be claimed again. [wasClaimed] tells whether it was claimed. */
    data class Offered(val record: CertificateRecord, val wasClaimed: Boolean) : GiveResult

    data object InProgress : GiveResult
}

/**
 * Issues, restores and hands out certificates.
 *
 * Only the metadata of all certificates is kept in memory. A certificate map gets its renderer when Minecraft loads
 * the map, and the image is read from disk only when the map is drawn for the first time, so unused certificates cost
 * almost nothing.
 */
object CertificateService {
    private const val PARALLEL_ISSUES = 4

    private val storage by lazy { CertificateStorage(plugin.dataPath) }
    private val registry = CertificateRegistry()
    private val claims = CertificateClaims(registry, ::persistClaim)

    /**
     * Loads the metadata of all stored certificates during plugin enable and then checks the world's map ids on the
     * global region, see [repairMapIds].
     */
    suspend fun restore() {
        val loaded = withContext(Dispatchers.IO) { storage.loadRecords() }
        loaded.failures.forEach { failure ->
            plugin.logger.warning("Could not load certificate ${failure.file}: ${failure.reason}")
            failure.mapId?.let(registry::addUnreadable)
        }
        loaded.records.forEach(registry::add)
        plugin.logger.info("Loaded ${loaded.records.size} certificate(s)")

        plugin.launch(plugin.globalRegionDispatcher) {
            try {
                repairMapIds()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                plugin.logger.log(Level.SEVERE, "Could not check the map ids of the certificates", exception)
            }
        }
    }

    /**
     * Attaches the certificate renderer when Minecraft loads the map of a certificate, before anyone sees it.
     *
     * Certificate maps are always locked. A map that is not locked has just been created; if its id belongs to a
     * certificate, the world's map data is older than the certificates and the map is someone else's, so it is left
     * alone.
     */
    fun onMapInitialize(map: MapView) {
        if (map.isLocked && map.id in registry) {
            attach(map)
        }
    }

    /** Whether [item] is a certificate map or a copy of one. */
    fun isCertificate(item: ItemStack?): Boolean {
        val mapId = item?.getData(DataComponentTypes.MAP_ID)?.id() ?: return false
        return mapId in registry
    }

    fun recordOf(mapId: Int) = registry[mapId]

    fun pendingFor(playerUuid: UUID) = registry.pendingFor(playerUuid)

    fun hasCertificate(playerUuid: UUID, eventName: String, date: LocalDate) =
        registry.hasCertificate(playerUuid, eventName, date)

    /**
     * Gives a single player the certificate for an event. If the player already has it, the same certificate is
     * offered again, so a lost certificate can be replaced. [onIssuing] is called before a new certificate is created.
     */
    suspend fun give(
        playerUuid: UUID,
        playerName: String,
        eventName: String,
        date: LocalDate,
        createdBy: UUID,
        onIssuing: () -> Unit,
    ): GiveResult = when (val reservation = registry.reserve(playerUuid, eventName, date)) {
        Reservation.Reserved -> {
            GiveResult.Issued(issueReserved(playerUuid, playerName, eventName, date, createdBy, onIssuing))
        }

        is Reservation.Exists -> offerAgain(reservation.record)
        Reservation.InProgress -> GiveResult.InProgress
    }

    /**
     * Issues certificates to all [participants], a few at a time to stay within the skin server's rate limits, and
     * calls [onIssued] for each certificate as soon as it is stored. Participants that already have a certificate for
     * this event, or are getting one right now, are skipped.
     */
    suspend fun issueAll(
        participants: List<NamedParticipant>,
        eventName: String,
        date: LocalDate,
        createdBy: UUID,
        onIssued: (CertificateRecord) -> Unit
    ): BulkIssueResult = coroutineScope {
        val semaphore = Semaphore(PARALLEL_ISSUES)
        val issued = Collections.synchronizedList(mutableListOf<CertificateRecord>())
        val skipped = Collections.synchronizedList(mutableListOf<NamedParticipant>())
        val failed = Collections.synchronizedList(mutableListOf<NamedParticipant>())
        val withoutSkin = Collections.synchronizedList(mutableListOf<NamedParticipant>())

        participants.map { participant ->
            async {
                semaphore.withPermit {
                    if (registry.reserve(participant.uuid, eventName, date) != Reservation.Reserved) {
                        skipped += participant
                        return@withPermit
                    }

                    try {
                        val certificate =
                            issueReserved(participant.uuid, participant.name, eventName, date, createdBy)
                        issued += certificate.record
                        if (certificate.skinUnavailable) {
                            withoutSkin += participant
                        }
                        onIssued(certificate.record)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        plugin.logger.log(
                            Level.SEVERE,
                            "Failed to create a certificate for ${participant.name}",
                            exception
                        )
                        failed += participant
                    }
                }
            }
        }.awaitAll()

        BulkIssueResult(issued.toList(), skipped.toList(), failed.toList(), withoutSkin.toList())
    }

    /**
     * Puts the pending certificate [mapId] into the inventory of [player], who must own it. Must run on the thread
     * of [player].
     */
    fun claim(player: Player, mapId: Int): ClaimResult = claims.claim(
        playerUuid = player.uniqueId,
        mapId = mapId,
        handOver = { record ->
            val inventory = player.inventory
            inventory.firstEmpty() != -1 && inventory.addItem(CertificateItem.create(record)).isEmpty()
        },
        savePlayer = {
            try {
                player.saveData()
            } catch (exception: Exception) {
                plugin.logger.log(Level.WARNING, "Could not save ${player.name} after claiming map #$mapId", exception)
            }
        },
    )

    /**
     * Creates the certificate reserved for the player: compose the image, create and configure a new map and persist
     * both. If anything fails, the reservation is released and nothing is registered; the unused map id is lost.
     */
    private suspend fun issueReserved(
        playerUuid: UUID,
        playerName: String,
        eventName: String,
        date: LocalDate,
        createdBy: UUID,
        onIssuing: () -> Unit = {},
    ): IssuedCertificate {
        var registered = false
        try {
            onIssuing()
            val face = SkinLoader.loadFace(playerUuid)
            val image = withContext(Dispatchers.Default) {
                CertificateArtwork.compose(
                    eventName,
                    playerName,
                    date,
                    (face as? SkinFace.Loaded)?.image ?: SkinHead.fallbackFace()
                )
            }

            val mapId = withContext(plugin.globalRegionDispatcher) { createUnusedMap().id }
            val record = CertificateRecord(
                mapId = mapId,
                playerUuid = playerUuid,
                playerName = playerName,
                eventName = eventName,
                date = date,
                createdBy = createdBy,
                createdAt = Instant.now(),
            )
            withContext(Dispatchers.IO) { storage.save(record, image) }
            registry.add(record)
            registered = true

            return IssuedCertificate(record, skinUnavailable = face == SkinFace.Unavailable)
        } finally {
            if (!registered) {
                registry.release(playerUuid, eventName, date)
            }
        }
    }

    private suspend fun offerAgain(record: CertificateRecord): GiveResult.Offered {
        if (!record.isClaimed) {
            return GiveResult.Offered(record, wasClaimed = false)
        }

        val pending = record.copy(claimedAt = null)
        withContext(Dispatchers.IO) { storage.saveRecord(pending) }
        registry.add(pending)
        return GiveResult.Offered(pending, wasClaimed = true)
    }

    private fun persistClaim(record: CertificateRecord) {
        try {
            storage.saveRecord(record)
        } catch (exception: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save the claim of certificate map #${record.mapId}", exception)
        }
    }

    /**
     * Makes sure that new maps get ids above all certificates. If the world's map data is older than the
     * certificates, for example after a crash before the next autosave or after restoring a world backup, Minecraft
     * would hand out the ids of certificates again. The certificate maps are created again on the way.
     */
    private fun repairMapIds() {
        val highest = registry.highestMapId() ?: return
        if (Bukkit.getMap(highest) != null) {
            return
        }

        plugin.logger.warning("The map of certificate #$highest is missing, the world's map data is probably older")
        val world = Bukkit.getWorlds().first()
        var restored = 0
        do {
            val map = Bukkit.createMap(world)
            if (map.id in registry) {
                attach(map)
                restored++
            }
        } while (map.id < highest)

        if (restored == 0) {
            plugin.logger.warning("The map data of certificate #$highest is lost, its map stays empty")
        } else {
            plugin.logger.info("Restored the maps of $restored certificate(s)")
        }
    }

    /**
     * Creates a map for a new certificate and attaches the certificate renderer. Ids that already belong to a
     * certificate are skipped; their certificate gets the freshly created map, which restores it.
     */
    private fun createUnusedMap(): MapView {
        val world = Bukkit.getWorlds().first()
        var map = Bukkit.createMap(world)
        while (map.id in registry) {
            plugin.logger.warning("Map #${map.id} already belongs to a certificate, restoring the certificate on it")
            attach(map)
            map = Bukkit.createMap(world)
        }
        attach(map)
        return map
    }

    private fun attach(map: MapView) {
        val mapId = map.id
        CertificateMapRenderer.attach(map) { renderer ->
            plugin.launch(Dispatchers.IO) {
                try {
                    renderer.provide(storage.loadImage(mapId))
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    plugin.logger.log(Level.WARNING, "Could not load the image of certificate map #$mapId", exception)
                }
            }
        }
    }
}
