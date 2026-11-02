package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import java.awt.image.BufferedImage

/**
 * Non-contextual renderer: all viewers share one canvas, which keeps its pixels between render calls.
 * The image is therefore drawn a single time; the flag is only set after drawing succeeded.
 *
 * A renderer instance belongs to exactly one map. Re-attaching creates a fresh canvas, so it needs a fresh renderer.
 */
class CertificateMapRenderer(private val image: BufferedImage) : MapRenderer(false) {
    @Volatile
    private var rendered = false

    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        if (rendered) return
        canvas.drawImage(0, 0, image)
        rendered = true
    }

    companion object {
        /**
         * Replaces every renderer of [map] with a certificate renderer and freezes the map, so the world is never
         * drawn into it. Does nothing if the map already shows a certificate.
         *
         * Must only be called while nobody can see the map yet: right after creating it, during server start or
         * from MapInitializeEvent.
         */
        fun attach(map: MapView, image: BufferedImage) {
            map.isLocked = true
            map.isTrackingPosition = false
            map.isUnlimitedTracking = false

            if (map.renderers.any { it is CertificateMapRenderer }) return
            map.renderers.forEach(map::removeRenderer)
            map.addRenderer(CertificateMapRenderer(image))
        }
    }
}
