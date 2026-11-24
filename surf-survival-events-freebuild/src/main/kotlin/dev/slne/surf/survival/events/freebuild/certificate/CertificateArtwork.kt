package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.Color
import java.awt.image.BufferedImage
import java.time.LocalDate
import javax.imageio.ImageIO

/**
 * Composes the 128x128 certificate image. The static artwork comes from images in `certificate/`: the template with
 * paper, frame and corner ornaments, and the divider. Title, event, head, name, date and the dividers between them
 * are placed on top as one block, centred vertically because its height depends on the event name.
 *
 * All colours are exact map palette colours, so the map shows them without any colour approximation. The player head
 * is rendered from the skin and matched to the palette here, without the colours of the paper, so it always stands out
 * from the background.
 */
object CertificateArtwork {
    const val SIZE = 128

    private const val TITLE = "TEILNAHMEURKUNDE"
    private const val CONTENT_WIDTH = 110
    private const val MAX_EVENT_LINES = 2
    private const val INNER_TOP = 5
    private const val INNER_HEIGHT = 118
    private const val LINE_GAP = 2
    private const val SECTION_GAP = 4
    private const val HEAD_GAP = 5
    private const val HEAD_SIZE = 40
    private const val HEAD_SIZE_COMPACT = 32
    private const val OUTLINE = 1

    private val INK = Color(0x4C3223)

    /** The paper colours of the template, which the head must not use. */
    private val HEAD_EXCLUDED_COLORS = setOf(0xF7E9A3, 0xD5C98C)

    private val template by lazy { loadImage("certificate-template.png") }
    private val divider by lazy { loadImage("certificate-divider.png") }

    fun compose(eventName: String, playerName: String, date: LocalDate, skin: BufferedImage): BufferedImage {
        val lineHeight = PixelFont.lineHeight
        val eventLines = PixelFont.wrap(eventName, CONTENT_WIDTH, MAX_EVENT_LINES, bold = false)
        val nameLine = PixelFont.wrap(playerName, CONTENT_WIDTH, 1, bold = false).firstOrNull().orEmpty()
        val headSize = if (eventLines.size > 1) HEAD_SIZE_COMPACT else HEAD_SIZE
        val head = SkinHead.render(skin, headSize - 2 * OUTLINE)
        val eventHeight = eventLines.size.coerceAtLeast(1).let { it * lineHeight + (it - 1) * LINE_GAP }

        val contentHeight = lineHeight + SECTION_GAP + divider.height + SECTION_GAP +
                eventHeight + HEAD_GAP + headSize + HEAD_GAP +
                lineHeight + SECTION_GAP + divider.height + SECTION_GAP + lineHeight
        var y = INNER_TOP + (INNER_HEIGHT - contentHeight) / 2

        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.drawImage(template, 0, 0, null)

            drawCentered(image, TITLE, y, bold = true)
            y += lineHeight + SECTION_GAP
            graphics.drawImage(divider, (SIZE - divider.width) / 2, y, null)
            y += divider.height + SECTION_GAP

            eventLines.forEachIndexed { index, line ->
                drawCentered(image, line, y + index * (lineHeight + LINE_GAP), bold = false)
            }
            y += eventHeight + HEAD_GAP

            drawHead(image, head, y + OUTLINE)
            y += headSize + HEAD_GAP

            drawCentered(image, nameLine, y, bold = false)
            y += lineHeight + SECTION_GAP
            graphics.drawImage(divider, (SIZE - divider.width) / 2, y, null)
            y += divider.height + SECTION_GAP

            drawCentered(image, CertificateDates.format(date), y, bold = false)
        } finally {
            graphics.dispose()
        }
        return image
    }

    /** Draws the head in map colours and surrounds its shape with a line of ink, which separates it from the paper. */
    private fun drawHead(image: BufferedImage, head: BufferedImage, top: Int) {
        val left = (SIZE - head.width) / 2
        val matched = HashMap<Int, Int>()
        fun isHead(x: Int, y: Int) = x in 0 until head.width && y in 0 until head.height && head.getRGB(x, y) ushr 24 != 0

        for (y in -OUTLINE until head.height + OUTLINE) {
            for (x in -OUTLINE until head.width + OUTLINE) {
                if (isHead(x, y)) {
                    val color = head.getRGB(x, y)
                    val mapColor = matched.getOrPut(color) { MapColors.nearest(color, HEAD_EXCLUDED_COLORS) }
                    image.setRGB(left + x, top + y, mapColor or 0xFF000000.toInt())
                } else if (isHead(x + 1, y) || isHead(x - 1, y) || isHead(x, y + 1) || isHead(x, y - 1)) {
                    image.setRGB(left + x, top + y, INK.rgb)
                }
            }
        }
    }

    private fun drawCentered(image: BufferedImage, text: String, y: Int, bold: Boolean) {
        val x = (SIZE - PixelFont.width(text, bold)) / 2
        PixelFont.draw(image, x, y, text, INK, bold)
    }

    private fun loadImage(name: String): BufferedImage {
        val stream = checkNotNull(CertificateArtwork::class.java.getResourceAsStream("/certificate/$name")) {
            "Missing resource certificate/$name"
        }
        return stream.use { ImageIO.read(it) }
    }
}
