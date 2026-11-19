package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.map.MapView
import java.util.logging.Logger

/** Outcome of [CertificateMaps.repair]. */
sealed interface MapRepair {
    data object NotNeeded : MapRepair

    /** The world's map ids were behind; the maps of [certificates] certificates were created again. */
    data class Restored(val certificates: Int) : MapRepair

    /** The world hands out [nextMapId] while certificates go up to [highestCertificate], too far to catch up. */
    data class TooFarBehind(val nextMapId: Int, val highestCertificate: Int) : MapRepair
}

/**
 * Creates the maps of certificates and keeps the world's map ids ahead of them.
 *
 * Minecraft saves its map data and its counter of map ids only on autosave, independently of each other, while
 * certificates are stored right away. After a crash or after restoring a world backup the world can therefore hand
 * out ids that already belong to certificates. A map with such an id shows the certificate: the certificate stays
 * intact and whoever got the new map holds a copy of it instead of an empty map.
 */
class CertificateMaps(
    private val registry: CertificateRegistry,
    private val createMap: () -> MapView,
    private val attach: (MapView) -> Unit,
    private val logger: Logger,
) {
    /**
     * Attaches the certificate renderer when Minecraft loads or creates the map of a certificate, before anyone sees
     * it. Certificate maps are stored locked, so an unlocked one was just created again with the id of a certificate.
     */
    fun onInitialize(map: MapView) {
        if (map.id !in registry) {
            return
        }

        if (!map.isLocked) {
            logger.warning("Map #${map.id} was created again although it is a certificate, it shows the certificate")
        }
        attach(map)
    }

    /** Creates the map of a new certificate. Ids of existing certificates are skipped and show their certificate. */
    fun createUnused(): MapView {
        var map = createMap()
        while (map.id in registry) {
            attach(map)
            map = createMap()
        }

        attach(map)
        return map
    }

    /**
     * Checks during startup whether the world hands out ids of existing certificates by creating one map, which costs
     * one id per start. If so, maps are created up to the highest certificate, which restores the certificates among
     * them. Gaps of more than [MAX_REPAIR_GAP] ids are only reported, since every created map stays in memory; their
     * certificates are still protected by [onInitialize] and [createUnused].
     */
    fun repair(): MapRepair {
        val highest = registry.highestMapId() ?: return MapRepair.NotNeeded
        var map = createMap()
        if (map.id > highest) {
            return MapRepair.NotNeeded
        }

        if (highest - map.id > MAX_REPAIR_GAP) {
            if (map.id in registry) {
                attach(map)
            }
            return MapRepair.TooFarBehind(map.id, highest)
        }

        var restored = 0
        while (true) {
            if (map.id in registry) {
                attach(map)
                restored++
            }
            if (map.id >= highest) {
                return MapRepair.Restored(restored)
            }
            map = createMap()
        }
    }

    companion object {
        const val MAX_REPAIR_GAP = 1_000
    }
}
