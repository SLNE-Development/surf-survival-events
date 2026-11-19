package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Non-contextual renderer: all viewers share one canvas, which keeps its pixels between render calls.
 *
 * The image is only requested from [requestImage] when the map is rendered for the first time, so certificates
 * nobody looks at never hold their image in memory. Once [provided][provide], the image is drawn a single time and
 * released; the flag is only set after drawing succeeded. If loading [failed][loadFailed], the image is requested
 * again after [RETRY_DELAY_MILLIS].
 *
 * A renderer instance belongs to exactly one map. Re-attaching creates a fresh canvas, so it needs a fresh renderer.
 */
class CertificateMapRenderer(
    private val clock: () -> Long = System::currentTimeMillis,
    private val requestImage: (CertificateMapRenderer) -> Unit,
) : MapRenderer(false) {
    private val requested = AtomicBoolean()

    @Volatile
    private var image: BufferedImage? = null

    @Volatile
    private var rendered = false

    @Volatile
    private var retryAt = 0L

    fun provide(image: BufferedImage) {
        if (!rendered) {
            this.image = image
        }
    }

    fun loadFailed() {
        retryAt = clock() + RETRY_DELAY_MILLIS
        requested.set(false)
    }

    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        if (rendered) return
        if (image == null && clock() >= retryAt && requested.compareAndSet(false, true)) {
            requestImage(this)
        }
        val image = image ?: return

        canvas.drawImage(0, 0, image)
        rendered = true
        this.image = null
    }

    companion object {
        const val RETRY_DELAY_MILLIS = 60_000L

        /**
         * Replaces every renderer of [map] with a certificate renderer and freezes the map, so the world is never
         * drawn into it. If the map already shows a certificate, its renderer is returned unchanged.
         *
         * Must only be called while nobody can see the map yet: right after creating it or from MapInitializeEvent.
         */
        fun attach(map: MapView, requestImage: (CertificateMapRenderer) -> Unit): CertificateMapRenderer {
            map.isLocked = true
            map.isTrackingPosition = false
            map.isUnlimitedTracking = false

            map.renderers.filterIsInstance<CertificateMapRenderer>().firstOrNull()?.let { return it }
            map.renderers.forEach(map::removeRenderer)

            return CertificateMapRenderer(requestImage = requestImage).also(map::addRenderer)
        }
    }
}
