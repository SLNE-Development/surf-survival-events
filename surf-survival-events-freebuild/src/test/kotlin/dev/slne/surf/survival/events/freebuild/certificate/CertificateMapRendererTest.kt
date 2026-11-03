package dev.slne.surf.survival.events.freebuild.certificate

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

class CertificateMapRendererTest {
    private val image = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
    private val view = mockk<MapView>(relaxed = true)
    private val player = mockk<Player>()

    private fun loadingRightAway() = CertificateMapRenderer { it.provide(image) }

    @Test
    fun `draws the certificate once and keeps the canvas afterwards`() {
        val canvas = mockk<MapCanvas>(relaxed = true)
        val renderer = loadingRightAway()

        repeat(3) { renderer.render(view, canvas, player) }

        verify(exactly = 1) { canvas.drawImage(0, 0, image) }
    }

    @Test
    fun `retries drawing when the first attempt failed`() {
        val canvas = mockk<MapCanvas>()
        var attempts = 0
        every { canvas.drawImage(0, 0, image) } answers { if (++attempts == 1) error("boom") }
        val renderer = loadingRightAway()

        assertFailsWith<IllegalStateException> { renderer.render(view, canvas, player) }
        renderer.render(view, canvas, player)
        renderer.render(view, canvas, player)

        assertEquals(2, attempts)
    }

    @Test
    fun `renders the same picture for every viewer`() {
        assertFalse(loadingRightAway().isContextual)
    }

    @Test
    fun `requests the image on the first render and only once`() {
        val canvas = mockk<MapCanvas>(relaxed = true)
        var requests = 0
        val renderer = CertificateMapRenderer { requests++ }
        assertEquals(0, requests)

        repeat(3) { renderer.render(view, canvas, player) }

        assertEquals(1, requests)
        verify(exactly = 0) { canvas.drawImage(any(), any(), any()) }
    }

    @Test
    fun `draws the image once it arrives and releases it`() {
        val canvas = mockk<MapCanvas>(relaxed = true)
        var requests = 0
        val renderer = CertificateMapRenderer { requests++ }
        renderer.render(view, canvas, player)

        renderer.provide(image)
        renderer.render(view, canvas, player)
        renderer.render(view, canvas, player)

        verify(exactly = 1) { canvas.drawImage(0, 0, image) }
        assertEquals(1, requests)
    }

    @Test
    fun `requests the image again a while after loading it failed`() {
        val canvas = mockk<MapCanvas>(relaxed = true)
        var now = 0L
        var requests = 0
        val renderer = CertificateMapRenderer(clock = { now }) { requests++ }
        renderer.render(view, canvas, player)

        renderer.loadFailed()
        now += CertificateMapRenderer.RETRY_DELAY_MILLIS - 1
        renderer.render(view, canvas, player)
        assertEquals(1, requests)

        now += 1
        renderer.render(view, canvas, player)
        renderer.render(view, canvas, player)
        assertEquals(2, requests)
    }

    @Test
    fun `ignores an image provided after drawing`() {
        val canvas = mockk<MapCanvas>(relaxed = true)
        val renderer = loadingRightAway()
        renderer.render(view, canvas, player)

        renderer.provide(BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB))
        renderer.render(view, canvas, player)

        verify(exactly = 1) { canvas.drawImage(any(), any(), any()) }
    }

    @Test
    fun `attaching replaces the vanilla renderer and locks the map without loading the image`() {
        val renderers = mutableListOf<MapRenderer>(mockk<MapRenderer>())
        val map = fakeMapView(renderers)
        var requests = 0

        CertificateMapRenderer.attach(map) { requests++ }

        assertIs<CertificateMapRenderer>(renderers.single())
        assertEquals(0, requests)
        verify { map.isLocked = true }
        verify { map.isTrackingPosition = false }
        verify { map.isUnlimitedTracking = false }
    }

    @Test
    fun `attaching twice keeps and returns a single certificate renderer`() {
        val renderers = mutableListOf<MapRenderer>()
        val map = fakeMapView(renderers)

        val first = CertificateMapRenderer.attach(map) {}
        val second = CertificateMapRenderer.attach(map) {}

        assertEquals(listOf<MapRenderer>(first), renderers)
        assertSame(first, second)
    }

    private fun fakeMapView(renderers: MutableList<MapRenderer>) = mockk<MapView>(relaxed = true) {
        every { getRenderers() } answers { renderers.toList() }
        every { addRenderer(any()) } answers { renderers += firstArg<MapRenderer>() }
        every { removeRenderer(any()) } answers { renderers.remove(firstArg<MapRenderer>()) }
    }
}
