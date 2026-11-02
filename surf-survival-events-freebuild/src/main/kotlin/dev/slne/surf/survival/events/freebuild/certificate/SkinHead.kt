package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/** Extracts the front of a player's head (base layer plus hat layer) from a skin texture. */
object SkinHead {
    const val FACE_SIZE = 8

    private const val SKIN_WIDTH = 64
    private const val MODERN_HEIGHT = 64
    private const val LEGACY_HEIGHT = 32
    private const val FACE_X = 8
    private const val FACE_Y = 8
    private const val HAT_X = 40
    private const val HAT_Y = 8
    private const val OPAQUE = 0xFF000000.toInt()
    private const val FALLBACK_RESOURCE = "/certificate/fallback-face.png"

    /** A Steve-like face bundled with the plugin, used when a skin cannot be loaded. */
    fun fallbackFace(): BufferedImage {
        val stream = checkNotNull(SkinHead::class.java.getResourceAsStream(FALLBACK_RESOURCE)) {
            "Missing resource $FALLBACK_RESOURCE"
        }
        val image = stream.use { ImageIO.read(it) }
        return BufferedImage(FACE_SIZE, FACE_SIZE, BufferedImage.TYPE_INT_ARGB).also { face ->
            for (y in 0 until FACE_SIZE) for (x in 0 until FACE_SIZE) face.setRGB(x, y, image.getRGB(x, y) or OPAQUE)
        }
    }

    fun extractFace(skin: BufferedImage): BufferedImage {
        require(skin.width == SKIN_WIDTH && (skin.height == MODERN_HEIGHT || skin.height == LEGACY_HEIGHT)) {
            "Not a skin texture: ${skin.width}x${skin.height}"
        }

        val face = BufferedImage(FACE_SIZE, FACE_SIZE, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until FACE_SIZE) {
            for (x in 0 until FACE_SIZE) {
                face.setRGB(x, y, skin.getRGB(FACE_X + x, FACE_Y + y) or OPAQUE)
            }
        }

        if (hasUsableOverlay(skin)) {
            val hat = skin.getSubimage(HAT_X, HAT_Y, FACE_SIZE, FACE_SIZE)
            val graphics = face.createGraphics()
            try {
                graphics.composite = AlphaComposite.SrcOver
                graphics.drawImage(hat, 0, 0, null)
            } finally {
                graphics.dispose()
            }
        }

        return face
    }

    fun scale(image: BufferedImage, factor: Int): BufferedImage {
        require(factor >= 1) { "Scale factor must be positive: $factor" }

        val scaled = BufferedImage(image.width * factor, image.height * factor, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                scaled.setRGB(x, y, image.getRGB(x / factor, y / factor))
            }
        }
        return scaled
    }

    /**
     * Legacy 64x32 skins often fill the hat area with an opaque colour. The game ignores the overlay in that case
     * (it only uses it if the area contains transparency), so we do the same.
     */
    private fun hasUsableOverlay(skin: BufferedImage): Boolean {
        if (skin.height != LEGACY_HEIGHT) return true

        for (y in 0 until LEGACY_HEIGHT) {
            for (x in SKIN_WIDTH / 2 until SKIN_WIDTH) {
                if ((skin.getRGB(x, y) ushr 24) < 128) return true
            }
        }
        return false
    }
}
