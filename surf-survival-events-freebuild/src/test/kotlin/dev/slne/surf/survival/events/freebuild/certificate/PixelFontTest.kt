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
        assertEquals(PixelFont.width("Event ? 2026", false), PixelFont.width("Event € 2026", false))

        val image = BufferedImage(128, 16, BufferedImage.TYPE_INT_ARGB)
        PixelFont.draw(image, 0, 0, "Event € 2026", ink, bold = false)
        assertTrue(inkColumns(image).second > 0)
    }

    @Test
    fun `lowercase umlauts are the base letter with two dots above`() {
        for ((umlaut, letter) in listOf("ä" to "a", "ö" to "o", "ü" to "u")) {
            val umlautRows = rows(umlaut)
            val letterRows = rows(letter)

            assertEquals(letterRows.drop(2), umlautRows.drop(2), "$umlaut below the dots")
            assertEquals(".#.#.", umlautRows[0].take(5), "$umlaut dots")
        }
    }

    @Test
    fun `uppercase umlauts carry two dots in their first row`() {
        for (umlaut in listOf("Ä", "Ö", "Ü")) {
            assertEquals("#...#", rows(umlaut)[0].take(5), umlaut)
        }
    }

    @Test
    fun `sharp s and typographic punctuation are written in their plain form`() {
        assertEquals("Strasse - \"Fest\" 's...", PixelFont.sanitize("Straße – „Fest“ ‚s…"))
        assertEquals(PixelFont.width("Strasse", bold = true), PixelFont.width("Straße", bold = true))
    }

    @Test
    fun `accented letters the font draws wrongly count as undrawable`() {
        assertEquals("Caf?", PixelFont.sanitize("Café"))
        assertEquals(setOf('é', '€'), PixelFont.undrawable("Café für 5 €"))
        assertEquals(emptySet(), PixelFont.undrawable("Straße – Größe Übung"))
    }

    private fun rows(text: String): List<String> {
        val image = BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB)
        PixelFont.draw(image, 0, 0, text, ink, bold = false)
        return (0 until 8).map { y -> (0 until 8).joinToString("") { x -> if (image.getRGB(x, y) == ink.rgb) "#" else "." } }
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
