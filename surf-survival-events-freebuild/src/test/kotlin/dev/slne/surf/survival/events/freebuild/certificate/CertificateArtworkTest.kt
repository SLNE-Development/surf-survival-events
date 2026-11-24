package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.image.BufferedImage
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CertificateArtworkTest {
    private val date = LocalDate.of(2026, 11, 2)
    private val paperColors = setOf(0xF7E9A3, 0xD5C98C)

    @Test
    fun `produces a fully opaque map sized image`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFF334CB2.toInt()))

        assertEquals(128, image.width)
        assertEquals(128, image.height)
        assertTrue(pixels(image).all { (it ushr 24) == 0xFF }, "every pixel must be opaque")
    }

    @Test
    fun `uses only map palette colours, the head included`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFFF8CF88.toInt()))

        val foreign = pixels(image).map { it and 0xFFFFFF }.toSet() - MapColors.all.toSet()
        assertTrue(foreign.isEmpty(), "unexpected colours: ${foreign.map { Integer.toHexString(it) }}")
    }

    @Test
    fun `a head in the colour of the paper still stands out from it`() {
        val reference = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFF334CB2.toInt()))
        val paperHead = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFFF7E9A3.toInt()))

        val headPixels = headPixels(reference, paperHead)
        assertTrue(headPixels.size > 500, "head found: ${headPixels.size} pixels")
        assertTrue(headPixels.none { (paperHead.getRGB(it.first, it.second) and 0xFFFFFF) in paperColors })
    }

    @Test
    fun `draws the head centered with at most 38 pixels plus its outline`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFF334CB2.toInt()))

        val box = headBox(image)
        assertTrue(maxOf(box.width, box.height) in 32..38, "head size ${box.width}x${box.height}")
        assertTrue(kotlin.math.abs(box.left - (127 - box.right)) <= 1, "head must be horizontally centered: $box")
    }

    @Test
    fun `outlines the head with ink`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFF334CB2.toInt()))
        val head = bluePixels(image)

        val outline = head.flatMap { (x, y) -> listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1) }.toSet() - head
        assertTrue(outline.isNotEmpty())
        assertTrue(outline.all { (x, y) -> image.getRGB(x, y) and 0xFFFFFF == 0x4C3223 }, "outline must be ink")
    }

    @Test
    fun `shrinks the head when the event name needs two lines`() {
        val single = headBox(
            CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, skin(0xFF334CB2.toInt()))
        )
        val compact = headBox(
            CertificateArtwork.compose(
                "Das grosse Sommerfest der Bauprofis 2026", "MaxMustermann", date, skin(0xFF334CB2.toInt())
            )
        )

        assertTrue(maxOf(compact.width, compact.height) <= 32, "compact head $compact")
        assertTrue(compact.width < single.width)
    }

    @Test
    fun `renders the fallback skin`() {
        val image = CertificateArtwork.compose("Winterevent 2026", "MaxMustermann", date, SkinHead.fallbackSkin())

        assertTrue(pixels(image).map { it and 0xFFFFFF }.toSet().size > 6)
    }

    @Test
    fun `long names never draw over the frame`() {
        val image = CertificateArtwork.compose(
            "Ein wirklich sehr langer Eventname der niemals auf zwei Zeilen passen kann",
            "WWWWWWWWWWWWWWWW",
            date,
            skin(0xFF334CB2.toInt())
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

    /** A skin whose whole head has [color], so the head can be told apart from the design. */
    private fun skin(color: Int) = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
        for (x in 0 until 32) for (y in 0 until 16) setRGB(x, y, color)
    }

    private fun pixels(image: BufferedImage) = image.getRGB(0, 0, image.width, image.height, null, 0, image.width).asList()

    private fun headPixels(first: BufferedImage, second: BufferedImage) = buildList {
        for (y in 0 until 128) for (x in 0 until 128) {
            if (first.getRGB(x, y) != second.getRGB(x, y)) add(x to y)
        }
    }

    private fun headBox(image: BufferedImage): Box {
        val head = bluePixels(image)
        return Box(head.minOf { it.first }, head.minOf { it.second }, head.maxOf { it.first }, head.maxOf { it.second })
    }

    /** The blue test head only uses blue map colours and the design none, so everything blue belongs to the head. */
    private fun bluePixels(image: BufferedImage): Set<Pair<Int, Int>> = buildSet {
        for (y in 0 until 128) for (x in 0 until 128) {
            val color = image.getRGB(x, y)
            if ((color and 0xFF) > (color shr 16 and 0xFF) + 40) add(x to y)
        }
    }

    private data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left + 1
        val height get() = bottom - top + 1
    }
}
