package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.image.BufferedImage
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CertificateArtworkTest {
    /** Unique colour that the design itself never uses, so the head can be located in the result. */
    private val faceColor = 0xFF1E3A8A.toInt()
    private val date = LocalDate.of(2026, 11, 2)

    /** Map palette colours (verified against MapPalette of paper-api 26.3): sand high/normal, podzol high, brown terracotta high. */
    private val designColors = setOf(0xF7E9A3, 0xD5C98C, 0x815631, 0x4C3223)

    @Test
    fun `produces a fully opaque map sized image`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, face())

        assertEquals(128, image.width)
        assertEquals(128, image.height)
        assertTrue(pixels(image).all { (it ushr 24) == 0xFF }, "every pixel must be opaque")
    }

    @Test
    fun `uses only map palette colours besides the head`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, face())

        val foreign = pixels(image).map { it and 0xFFFFFF }.toSet() - designColors - (faceColor and 0xFFFFFF)
        assertTrue(foreign.isEmpty(), "unexpected colours: ${foreign.map { Integer.toHexString(it) }}")
    }

    @Test
    fun `draws the head as a sharp centered 40 pixel block`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, face())

        val box = boundingBox(image, faceColor)
        assertEquals(40, box.width)
        assertEquals(40, box.height)
        assertEquals(40 * 40, pixels(image).count { it == faceColor })
        assertEquals(box.left, 127 - box.right, "head must be horizontally centered")
    }

    @Test
    fun `shrinks the head when the event name needs two lines`() {
        val image = CertificateArtwork.compose("Das grosse Sommerfest der Bauprofis 2026", "MaxMustermann", date, face())

        val box = boundingBox(image, faceColor)
        assertEquals(32, box.width)
        assertEquals(32, box.height)
    }

    @Test
    fun `long names never draw over the frame`() {
        val image = CertificateArtwork.compose(
            "Ein wirklich sehr langer Eventname der niemals auf zwei Zeilen passen kann",
            "WWWWWWWWWWWWWWWW",
            date,
            face()
        )

        val frame = buildList {
            for (i in 0 until 128) {
                for (inset in 0..1) {
                    add(image.getRGB(i, inset)); add(image.getRGB(i, 127 - inset))
                    add(image.getRGB(inset, i)); add(image.getRGB(127 - inset, i))
                }
            }
        }
        assertEquals(setOf(0x815631), frame.map { it and 0xFFFFFF }.toSet())
    }

    private fun face() = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB).apply {
        for (x in 0 until 8) for (y in 0 until 8) setRGB(x, y, faceColor)
    }

    private fun pixels(image: BufferedImage) = image.getRGB(0, 0, image.width, image.height, null, 0, image.width).asList()

    private data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left + 1
        val height get() = bottom - top + 1
    }

    private fun boundingBox(image: BufferedImage, color: Int): Box {
        var left = Int.MAX_VALUE; var top = Int.MAX_VALUE; var right = -1; var bottom = -1
        for (x in 0 until image.width) for (y in 0 until image.height) {
            if (image.getRGB(x, y) == color) {
                left = minOf(left, x); top = minOf(top, y); right = maxOf(right, x); bottom = maxOf(bottom, y)
            }
        }
        return Box(left, top, right, bottom)
    }
}
