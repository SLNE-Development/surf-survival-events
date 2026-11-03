package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Renders a player's head from the skin texture as a small 3D view that looks to the right: the face straight from
 * the front, with the right side and the top as narrow, shaded strips receding to the upper left. The hat layer lies
 * directly on the head.
 *
 * Every pixel of the face covers the same whole number of map pixels, so both eyes always have the same size. Only the
 * strips are drawn at four times the size and reduced by giving every pixel the colour that covers most of it, which
 * keeps blends of neighbouring skin pixels, and with them unrelated map colours, out of the result.
 */
object SkinHead {
    private const val SKIN_WIDTH = 64
    private const val MODERN_HEIGHT = 64
    private const val LEGACY_HEIGHT = 32
    private const val FACE_SIZE = 8
    private const val HAT_OFFSET = 32
    private const val OPAQUE = 0xFF000000.toInt()
    private const val FALLBACK_RESOURCE = "/certificate/fallback-face.png"
    private const val SUPERSAMPLING = 4
    private const val SIDE_SHADE = 0.75
    private const val TOP_SHADE = 1.1

    /**
     * The face takes eight pixels of the skin and the strips one more, so the head is nine skin pixels wide and high;
     * each of them covers `size / 9` map pixels.
     */
    private const val HEAD_SPAN = FACE_SIZE + 1

    /** Renders the head of [skin] centred into a transparent image of [size] x [size] pixels. */
    fun render(skin: BufferedImage, size: Int): BufferedImage {
        requireSkin(skin)
        val pixel = size / HEAD_SPAN
        require(pixel > 0) { "Size too small for a head: $size" }

        val depth = pixel.toDouble() / FACE_SIZE
        val margin = (size - pixel * HEAD_SPAN) / 2
        val faceLeft = (margin + pixel).toDouble()
        val faceTop = (margin + pixel).toDouble()
        val backLeft = faceLeft - FACE_SIZE * depth
        val backTop = faceTop - FACE_SIZE * depth

        val canvas = BufferedImage(size * SUPERSAMPLING, size * SUPERSAMPLING, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)

            val hat = hasUsableOverlay(skin)
            draw(graphics, texture(skin, 0, 8, SIDE_SHADE, hat), depth, depth, 0.0, pixel.toDouble(), backLeft, backTop)
            draw(graphics, texture(skin, 8, 0, TOP_SHADE, hat), pixel.toDouble(), 0.0, depth, depth, backLeft, backTop)
            draw(graphics, texture(skin, 8, 8, 1.0, hat), pixel.toDouble(), 0.0, 0.0, pixel.toDouble(), faceLeft, faceTop)
        } finally {
            graphics.dispose()
        }

        return reduce(canvas, size)
    }

    /**
     * A head built from the Steve-like face bundled with the plugin, used when a skin cannot be loaded. The sides and
     * the top continue the edges of the face.
     */
    fun fallbackSkin(): BufferedImage {
        val stream = checkNotNull(SkinHead::class.java.getResourceAsStream(FALLBACK_RESOURCE)) {
            "Missing resource $FALLBACK_RESOURCE"
        }
        val face = stream.use { ImageIO.read(it) }

        return BufferedImage(SKIN_WIDTH, MODERN_HEIGHT, BufferedImage.TYPE_INT_ARGB).also { skin ->
            for (y in 0 until FACE_SIZE) {
                for (x in 0 until FACE_SIZE) {
                    skin.setRGB(8 + x, 8 + y, face.getRGB(x, y) or OPAQUE)
                    skin.setRGB(x, 8 + y, face.getRGB(0, y) or OPAQUE)
                    skin.setRGB(16 + x, 8 + y, face.getRGB(FACE_SIZE - 1, y) or OPAQUE)
                    skin.setRGB(8 + x, y, face.getRGB(x, 0) or OPAQUE)
                }
            }
        }
    }

    fun requireSkin(skin: BufferedImage): BufferedImage = skin.also {
        require(it.width == SKIN_WIDTH && (it.height == MODERN_HEIGHT || it.height == LEGACY_HEIGHT)) {
            "Not a skin texture: ${it.width}x${it.height}"
        }
    }

    /** One side of the head: the base layer, opaque like in game, covered by the opaque pixels of the hat layer. */
    private fun texture(skin: BufferedImage, left: Int, top: Int, shade: Double, hat: Boolean): BufferedImage {
        val texture = BufferedImage(FACE_SIZE, FACE_SIZE, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until FACE_SIZE) {
            for (x in 0 until FACE_SIZE) {
                val overlay = skin.getRGB(left + HAT_OFFSET + x, top + y)
                val color = if (hat && overlay ushr 24 >= 128) overlay else skin.getRGB(left + x, top + y)
                texture.setRGB(x, y, shade(color, shade))
            }
        }
        return texture
    }

    /**
     * Draws [texture] so that one skin pixel step to the right moves by ([ux], [uy]) map pixels and one step down by
     * ([vx], [vy]), starting at ([left], [top]).
     */
    private fun draw(
        graphics: Graphics2D,
        texture: BufferedImage,
        ux: Double,
        uy: Double,
        vx: Double,
        vy: Double,
        left: Double,
        top: Double,
    ) {
        val transform = AffineTransform(ux, uy, vx, vy, left, top)
        transform.preConcatenate(AffineTransform.getScaleInstance(SUPERSAMPLING.toDouble(), SUPERSAMPLING.toDouble()))
        graphics.drawImage(texture, transform, null)
    }

    /** Gives every pixel the colour covering most of it; pixels covered less than half stay transparent. */
    private fun reduce(canvas: BufferedImage, size: Int): BufferedImage {
        val result = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val samples = SUPERSAMPLING * SUPERSAMPLING
        val counts = LinkedHashMap<Int, Int>()

        for (y in 0 until size) {
            for (x in 0 until size) {
                counts.clear()
                for (sy in 0 until SUPERSAMPLING) {
                    for (sx in 0 until SUPERSAMPLING) {
                        val color = canvas.getRGB(x * SUPERSAMPLING + sx, y * SUPERSAMPLING + sy)
                        if (color ushr 24 != 0) {
                            counts.merge(color, 1, Int::plus)
                        }
                    }
                }

                if (counts.values.sum() * 2 >= samples) {
                    result.setRGB(x, y, counts.maxBy { it.value }.key)
                }
            }
        }
        return result
    }

    private fun shade(color: Int, factor: Double): Int {
        fun channel(shift: Int) = ((color shr shift and 0xFF) * factor).roundToInt().coerceAtMost(255)
        return OPAQUE or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
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
