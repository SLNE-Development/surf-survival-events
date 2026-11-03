package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkinHeadTest {
    private val front = 0xFFFF0000.toInt()
    private val rightSide = 0xFF00FF00.toInt()
    private val leftSide = 0xFFFFFF00.toInt()
    private val top = 0xFF0000FF.toInt()
    private val back = 0xFFFF00FF.toInt()
    private val bottom = 0xFF00FFFF.toInt()
    private val hat = 0xFFFFFFFF.toInt()

    private val shadedRightSide = 0xFF00BF00.toInt()
    private val shadedLeftSide = 0xFFBFBF00.toInt()

    @Test
    fun `renders the front, the right side and the top of the head`() {
        val colors = opaqueColors(SkinHead.render(skin(), 40))

        assertEquals(setOf(front, shadedRightSide, top), colors.toSet())
    }

    @Test
    fun `hides the sides that face away`() {
        val colors = opaqueColors(SkinHead.render(skin(), 40)).toSet()

        assertFalse(shadedLeftSide in colors || back in colors || bottom in colors)
    }

    @Test
    fun `the head looks to the right and is seen from above`() {
        val head = SkinHead.render(skin(), 40)

        assertTrue(center(head, front).first > center(head, shadedRightSide).first, "front is right of the side")
        assertTrue(center(head, top).second < center(head, front).second, "top is above the front")
    }

    @Test
    fun `every pixel of the face becomes an equal square, so both eyes have the same size`() {
        val leftEye = 0xFF101010.toInt()
        val rightEye = 0xFF202020.toInt()
        for (size in 30..40) {
            val skin = skin()
            skin.setRGB(8 + 1, 8 + 4, leftEye)
            skin.setRGB(8 + 6, 8 + 4, rightEye)

            val pixels = SkinHead.render(skin, size).let { it.getRGB(0, 0, it.width, it.height, null, 0, it.width) }
            val texel = size / 9

            assertEquals(texel * texel, pixels.count { it == leftEye }, "left eye at size $size")
            assertEquals(texel * texel, pixels.count { it == rightEye }, "right eye at size $size")
        }
    }

    @Test
    fun `has the requested size and a transparent background`() {
        val head = SkinHead.render(skin(), 40)

        assertEquals(40, head.width)
        assertEquals(40, head.height)
        assertEquals(0, head.getRGB(0, 0) ushr 24)
        assertEquals(0, head.getRGB(39, 39) ushr 24)
    }

    @Test
    fun `opaque hat pixels cover the face`() {
        val skin = skin()
        fill(skin, 40, 8, hat)

        val colors = opaqueColors(SkinHead.render(skin, 40)).toSet()

        assertTrue(hat in colors)
        assertFalse(front in colors)
    }

    @Test
    fun `transparent face pixels are drawn opaque like in game`() {
        val skin = skin()
        fill(skin, 8, 8, front and 0x00FFFFFF)

        assertTrue(front in opaqueColors(SkinHead.render(skin, 40)))
    }

    @Test
    fun `legacy skins ignore a completely opaque overlay area`() {
        val skin = skin(height = 32)
        for (y in 0 until 32) for (x in 32 until 64) skin.setRGB(x, y, hat)

        assertFalse(hat in opaqueColors(SkinHead.render(skin, 40)))
    }

    @Test
    fun `legacy skins use their overlay when it contains transparency`() {
        val skin = skin(height = 32)
        fill(skin, 40, 8, hat)

        assertTrue(hat in opaqueColors(SkinHead.render(skin, 40)))
    }

    @Test
    fun `images that are not skins are rejected`() {
        assertFailsWith<IllegalArgumentException> { SkinHead.render(BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), 40) }
    }

    @Test
    fun `the bundled fallback skin renders a complete head`() {
        val fallback = SkinHead.fallbackSkin()
        val colors = opaqueColors(SkinHead.render(fallback, 40))

        assertEquals(64, fallback.width)
        assertEquals(64, fallback.height)
        assertTrue(colors.size > 40 * 40 / 2, "head covers most of the image")
    }

    private fun skin(height: Int = 64) = BufferedImage(64, height, BufferedImage.TYPE_INT_ARGB).also { skin ->
        fill(skin, 8, 8, front)
        fill(skin, 0, 8, rightSide)
        fill(skin, 16, 8, leftSide)
        fill(skin, 24, 8, back)
        fill(skin, 8, 0, top)
        fill(skin, 16, 0, bottom)
    }

    private fun fill(skin: BufferedImage, left: Int, top: Int, color: Int) {
        for (y in top until top + 8) for (x in left until left + 8) skin.setRGB(x, y, color)
    }

    private fun opaqueColors(image: BufferedImage): List<Int> =
        image.getRGB(0, 0, image.width, image.height, null, 0, image.width).filter { it ushr 24 == 0xFF }

    private fun center(image: BufferedImage, color: Int): Pair<Double, Double> {
        val positions = buildList {
            for (y in 0 until image.height) for (x in 0 until image.width) {
                if (image.getRGB(x, y) == color) add(x to y)
            }
        }
        assertTrue(positions.isNotEmpty(), "colour ${Integer.toHexString(color)} not drawn")
        return positions.map { it.first }.average() to positions.map { it.second }.average()
    }
}
