package dev.slne.surf.survival.events.freebuild.certificate

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.survival.events.freebuild.plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapView
import java.awt.image.BufferedImage
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class IssuedCertificate(val record: CertificateRecord, val item: ItemStack)

object CertificateService {
    private val storage by lazy { CertificateStorage(plugin.dataPath) }

    /** Finished certificate images by map id. Filled on start and whenever a certificate is issued. */
    private val images = ConcurrentHashMap<Int, BufferedImage>()

    /**
     * Loads all stored certificates and attaches their renderers. Runs during plugin enable (MCCoroutine blocks
     * `onEnable` until this returns), i.e. before players can see any of these maps.
     */
    suspend fun restore() {
        val loaded = withContext(Dispatchers.IO) { storage.loadAll() }
        loaded.failures.forEach { plugin.logger.warning("Could not load certificate $it") }
        loaded.certificates.forEach { images[it.record.mapId] = it.image }

        var restored = 0
        for ((mapId, image) in images) {
            val map = Bukkit.getMap(mapId)
            if (map == null) {
                plugin.logger.warning("Certificate map #$mapId does not exist in the world (anymore)")
                continue
            }
            CertificateMapRenderer.attach(map, image)
            restored++
        }
        plugin.logger.info("Restored $restored certificate map(s)")
    }

    /** Safety net for certificate maps whose data is loaded after [restore], e.g. if the map data was not cached. */
    fun onMapInitialize(map: MapView) {
        images[map.id]?.let { CertificateMapRenderer.attach(map, it) }
    }

    /**
     * Creates a certificate: compose the image, create and configure a new map, persist both and only then hand
     * out the item. If persisting fails, no item is created (the unused map id is simply lost).
     */
    suspend fun issue(
        playerUuid: UUID,
        playerName: String,
        eventName: String,
        date: LocalDate,
        createdBy: UUID
    ): IssuedCertificate {
        val face = SkinLoader.loadFace(playerUuid)
        val image = withContext(Dispatchers.IO) {
            CertificateArtwork.compose(eventName, playerName, date, face ?: SkinHead.fallbackFace())
        }

        val map = withContext(plugin.globalRegionDispatcher) {
            val map = Bukkit.createMap(Bukkit.getWorlds().first())
            check(!images.containsKey(map.id)) {
                "New map #${map.id} already belongs to a stored certificate; the world's map data was probably reset"
            }
            CertificateMapRenderer.attach(map, image)
            map
        }

        val record = CertificateRecord(
            mapId = map.id,
            playerUuid = playerUuid,
            playerName = playerName,
            eventName = eventName,
            date = date,
            createdBy = createdBy,
            createdAt = Instant.now(),
        )
        withContext(Dispatchers.IO) { storage.save(record, image) }
        images[map.id] = image

        return IssuedCertificate(record, CertificateItem.create(map, record))
    }
}
