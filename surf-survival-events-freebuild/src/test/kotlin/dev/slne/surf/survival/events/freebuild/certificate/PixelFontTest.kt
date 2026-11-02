package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelFontTest {
    private val ink = Color(76, 50, 35)

    @Test
    fun `bold text is one pixel wider per character`() {
        val regular = PixelFont.width("Winter", bold = false)
        assertEquals(regular + 6, PixelFont.width("Winter", bold = true))
    }

    @Test
    fun `drawn pixels span exactly the measured width`() {
        for (bold in listOf(false, true)) {
            val image = BufferedImage(128, 16, BufferedImage.TYPE_INT_ARGB)
            PixelFont.draw(image, 3, 2, "TEILNAHMEURKUNDE", ink, bold)

            val (minX, maxX) = inkColumns(image)
            assertEquals(3, minX, "left edge (bold=$bold)")
            assertEquals(PixelFont.width("TEILNAHMEURKUNDE", bold), maxX - minX + 1, "width (bold=$bold)")
        }
    }

    @Test
    fun `characters the minecraft font does not know are replaced instead of failing`() {
        assertEquals(PixelFont.width("Event ? 2026", false), PixelFont.width("Event – 2026", false))

        val image = BufferedImage(128, 16, BufferedImage.TYPE_INT_ARGB)
        PixelFont.draw(image, 0, 0, "Event – 2026", ink, bold = false)
        assertTrue(inkColumns(image).second > 0)
    }

    @Test
    fun `german umlauts are drawn as themselves and not replaced`() {
        for (umlaut in listOf("Ä", "Ö", "Ü", "ä", "ö", "ü", "ß")) {
            assertTrue(!render(umlaut).contentEquals(render("?")), "$umlaut was replaced")
        }
    }

    private fun render(text: String): IntArray {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        PixelFont.draw(image, 0, 0, text, ink, bold = false)
        return image.getRGB(0, 0, 16, 16, null, 0, 16)
    }

    private fun inkColumns(image: BufferedImage): Pair<Int, Int> {
        var minX = Int.MAX_VALUE
        var maxX = -1
        for (x in 0 until image.width) for (y in 0 until image.height) {
            if (image.getRGB(x, y) == ink.rgb) {
                minX = minOf(minX, x)
                maxX = maxOf(maxX, x)
            }
        }
        return minX to maxX
    }
}
