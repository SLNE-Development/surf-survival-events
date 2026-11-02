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

class CertificateMapRendererTest {
    private val image = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
    private val view = mockk<MapView>(relaxed = true)
    private val player = mockk<Player>()

    @Test
    fun `draws the certificate once and keeps the canvas afterwards`() {
        val canvas = mockk<MapCanvas>(relaxed = true)
        val renderer = CertificateMapRenderer(image)

        repeat(3) { renderer.render(view, canvas, player) }

        verify(exactly = 1) { canvas.drawImage(0, 0, image) }
    }

    @Test
    fun `retries drawing when the first attempt failed`() {
        val canvas = mockk<MapCanvas>()
        var attempts = 0
        every { canvas.drawImage(0, 0, image) } answers { if (++attempts == 1) error("boom") }
        val renderer = CertificateMapRenderer(image)

        assertFailsWith<IllegalStateException> { renderer.render(view, canvas, player) }
        renderer.render(view, canvas, player)
        renderer.render(view, canvas, player)

        assertEquals(2, attempts)
    }

    @Test
    fun `renders the same picture for every viewer`() {
        assertFalse(CertificateMapRenderer(image).isContextual)
    }

    @Test
    fun `attaching replaces the vanilla renderer and locks the map`() {
        val renderers = mutableListOf<MapRenderer>(mockk<MapRenderer>())
        val map = fakeMapView(renderers)

        CertificateMapRenderer.attach(map, image)

        assertIs<CertificateMapRenderer>(renderers.single())
        verify { map.isLocked = true }
        verify { map.isTrackingPosition = false }
        verify { map.isUnlimitedTracking = false }
    }

    @Test
    fun `attaching twice keeps a single certificate renderer`() {
        val renderers = mutableListOf<MapRenderer>()
        val map = fakeMapView(renderers)

        CertificateMapRenderer.attach(map, image)
        val first = renderers.single()
        CertificateMapRenderer.attach(map, image)

        assertEquals(listOf(first), renderers)
    }

    private fun fakeMapView(renderers: MutableList<MapRenderer>) = mockk<MapView>(relaxed = true) {
        every { getRenderers() } answers { renderers.toList() }
        every { addRenderer(any()) } answers { renderers += firstArg<MapRenderer>() }
        every { removeRenderer(any()) } answers { renderers.remove(firstArg<MapRenderer>()) }
    }
}
