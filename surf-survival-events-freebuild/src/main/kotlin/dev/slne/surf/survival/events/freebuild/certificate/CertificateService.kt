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
    private const val PERSIST_LOCK_STRIPES = 64

    private val storage by lazy { CertificateStorage(plugin.dataPath) }
    private val registry = CertificateRegistry()
    private val claims = CertificateClaims(registry, ::persistClaim)
    private val maps by lazy {
        CertificateMaps(
            registry = registry,
            createMap = { Bukkit.createMap(Bukkit.getWorlds().first()) },
            attach = ::attach,
            logger = plugin.logger,
        )
    }
    private val persistLocks = Array(PERSIST_LOCK_STRIPES) { Any() }

    /**
     * Loads the metadata of all stored certificates during plugin enable and then checks the world's map ids on the
     * global region, see [CertificateMaps.repair].
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
                reportRepair(maps.repair())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                plugin.logger.log(Level.SEVERE, "Could not check the map ids of the certificates", exception)
            }
        }
    }

    /** Attaches the certificate renderer when Minecraft loads or creates the map of a certificate. */
    fun onMapInitialize(map: MapView) = maps.onInitialize(map)

    /** Whether [item] is a certificate map or a copy of one. */
    fun isCertificate(item: ItemStack?): Boolean {
        val mapId = item?.getData(DataComponentTypes.MAP_ID)?.id() ?: return false
        return mapId in registry
    }

    fun recordOf(mapId: Int) = registry[mapId]

    fun pendingFor(playerUuid: UUID) = registry.pendingFor(playerUuid)

    fun hasCertificate(playerUuid: UUID, eventName: String, date: LocalDate) =
        registry.hasCertificate(playerUuid, eventName, date)

    /** All certificates of [playerUuid], newest event first. */
    fun certificatesOf(playerUuid: UUID) = registry.certificatesOf(playerUuid)

    /** All certificates, newest event first. */
    fun allCertificates() = registry.all()

    /**
     * Puts a copy of [record] into the inventory of [player] and returns whether it fit. The copy shows the same map
     * and leaves the claim of the owner untouched. Must run on the thread of [player].
     */
    fun giveCopy(player: Player, record: CertificateRecord): Boolean =
        player.inventory.addItem(CertificateItem.create(record)).isEmpty()

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

        is Reservation.Exists -> offerAgain(reservation.record.mapId)
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

                    val certificate = try {
                        issueReserved(participant.uuid, participant.name, eventName, date, createdBy)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        plugin.logger.log(
                            Level.SEVERE,
                            "Failed to create a certificate for ${participant.name}",
                            exception
                        )
                        failed += participant
                        return@withPermit
                    }

                    issued += certificate.record
                    if (certificate.skinUnavailable) {
                        withoutSkin += participant
                    }
                    try {
                        onIssued(certificate.record)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        plugin.logger.log(Level.WARNING, "Could not notify ${participant.name}", exception)
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
        handOver = { record -> player.inventory.addItem(CertificateItem.create(record)).isEmpty() },
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
            val skin = SkinLoader.loadSkin(playerUuid)
            val image = withContext(Dispatchers.Default) {
                CertificateArtwork.compose(
                    eventName,
                    playerName,
                    date,
                    (skin as? PlayerSkin.Loaded)?.texture ?: SkinHead.fallbackSkin()
                )
            }

            val mapId = withContext(plugin.globalRegionDispatcher) { maps.createUnused().id }
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

            return IssuedCertificate(record, skinUnavailable = skin == PlayerSkin.Unavailable)
        } finally {
            if (!registered) {
                registry.release(playerUuid, eventName, date)
            }
        }
    }

    private suspend fun offerAgain(mapId: Int): GiveResult.Offered {
        val reopened = registry.reopen(mapId)
        if (reopened) {
            withContext(Dispatchers.IO) { persist(mapId) }
        }

        return GiveResult.Offered(checkNotNull(registry[mapId]), wasClaimed = reopened)
    }

    private fun persistClaim(record: CertificateRecord) {
        try {
            persist(record.mapId)
        } catch (exception: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to save the claim of certificate map #${record.mapId}", exception)
        }
    }

    /**
     * Writes the current state of certificate [mapId]. Writes of the same certificate never overlap and each one
     * reads the state only once it holds the lock, so the file always ends up with the latest state, even if a claim
     * and an offer of the same certificate happen at the same time.
     */
    private fun persist(mapId: Int) {
        synchronized(persistLocks[Math.floorMod(mapId, persistLocks.size)]) {
            registry[mapId]?.let(storage::saveRecord)
        }
    }

    private fun reportRepair(repair: MapRepair) {
        when (repair) {
            MapRepair.NotNeeded -> Unit
            is MapRepair.Restored -> plugin.logger.warning(
                "The world's map ids were behind the certificates, restored the maps of ${repair.certificates} " +
                        "certificate(s)"
            )

            is MapRepair.TooFarBehind -> plugin.logger.severe(
                "The world hands out map #${repair.nextMapId}, but certificates go up to " +
                        "#${repair.highestCertificate}. The gap is too large to catch up automatically; new maps " +
                        "with the id of a certificate show that certificate."
            )
        }
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
                    renderer.loadFailed()
                }
            }
        }
    }
}
