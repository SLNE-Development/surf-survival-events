package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SkinHeadTest {
    private val skinTone = 0xFFC08A6C.toInt()
    private val hairColor = 0xFF3A2A12.toInt()
    private val hatColor = 0xFF1E3A8A.toInt()

    @Test
    fun `face is taken from the front of the head`() {
        val skin = skin(64, 64)
        skin.setRGB(8, 8, skinTone)
        skin.setRGB(15, 15, hairColor)

        val face = SkinHead.extractFace(skin)

        assertEquals(8, face.width)
        assertEquals(8, face.height)
        assertEquals(skinTone, face.getRGB(0, 0))
        assertEquals(hairColor, face.getRGB(7, 7))
    }

    @Test
    fun `opaque overlay pixels cover the face`() {
        val skin = skin(64, 64)
        skin.setRGB(9, 9, skinTone)
        skin.setRGB(41, 9, hatColor)

        assertEquals(hatColor, SkinHead.extractFace(skin).getRGB(1, 1))
    }

    @Test
    fun `transparent overlay pixels let the face show through`() {
        val skin = skin(64, 64)
        skin.setRGB(9, 9, skinTone)

        assertEquals(skinTone, SkinHead.extractFace(skin).getRGB(1, 1))
    }

    @Test
    fun `transparent face pixels become opaque like in game`() {
        val skin = skin(64, 64)
        skin.setRGB(10, 10, 0x00C08A6C)

        assertEquals(skinTone, SkinHead.extractFace(skin).getRGB(2, 2))
    }

    @Test
    fun `legacy skins use their overlay when it contains transparency`() {
        val skin = skin(64, 32)
        skin.setRGB(9, 9, skinTone)
        skin.setRGB(41, 9, hatColor)

        assertEquals(hatColor, SkinHead.extractFace(skin).getRGB(1, 1))
    }

    @Test
    fun `legacy skins ignore a completely opaque overlay area`() {
        val skin = skin(64, 32)
        skin.setRGB(9, 9, skinTone)
        for (x in 32 until 64) for (y in 0 until 32) skin.setRGB(x, y, 0xFF000000.toInt())

        assertEquals(skinTone, SkinHead.extractFace(skin).getRGB(1, 1))
    }

    @Test
    fun `images that are not skins are rejected`() {
        assertFailsWith<IllegalArgumentException> { SkinHead.extractFace(skin(16, 16)) }
    }

    @Test
    fun `scaling repeats every pixel as a sharp block`() {
        val source = BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB)
        source.setRGB(0, 0, skinTone)
        source.setRGB(1, 0, hatColor)

        val scaled = SkinHead.scale(source, 3)

        assertEquals(6, scaled.width)
        assertEquals(3, scaled.height)
        for (y in 0 until 3) {
            for (x in 0 until 3) assertEquals(skinTone, scaled.getRGB(x, y), "($x,$y)")
            for (x in 3 until 6) assertEquals(hatColor, scaled.getRGB(x, y), "($x,$y)")
        }
    }

    @Test
    fun `bundled fallback face is an opaque 8x8 face`() {
        val face = SkinHead.fallbackFace()

        assertEquals(8, face.width)
        assertEquals(8, face.height)
        for (x in 0 until 8) for (y in 0 until 8) assertEquals(0xFF, face.getRGB(x, y) ushr 24, "($x,$y)")
    }

    private fun skin(width: Int, height: Int) = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
}
