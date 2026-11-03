package dev.slne.surf.survival.events.freebuild.certificate

import java.awt.Color
import java.awt.image.BufferedImage
import java.time.LocalDate

/**
 * Composes the 128x128 certificate image. All design colours are exact map palette colours, so the map shows them
 * without any colour approximation. The player head is rendered from the skin and matched to the palette here,
 * without the colours of the paper, so it always stands out from the background.
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
    private const val DIVIDER_HEIGHT = 3
    private const val HEAD_SIZE = 40
    private const val HEAD_SIZE_COMPACT = 32
    private const val OUTLINE = 1

    private val PARCHMENT = Color(0xF7E9A3)
    private val PARCHMENT_SHADE = Color(0xD5C98C)
    private val BORDER = Color(0x815631)
    private val INK = Color(0x4C3223)
    private val HEAD_EXCLUDED_COLORS = setOf(PARCHMENT.rgb and 0xFFFFFF, PARCHMENT_SHADE.rgb and 0xFFFFFF)

    fun compose(eventName: String, playerName: String, date: LocalDate, skin: BufferedImage): BufferedImage {
        val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
        drawBackground(image)

        val lineHeight = PixelFont.lineHeight
        val eventLines = PixelFont.wrap(eventName, CONTENT_WIDTH, MAX_EVENT_LINES, bold = false)
        val nameLine = PixelFont.wrap(playerName, CONTENT_WIDTH, 1, bold = false).firstOrNull().orEmpty()
        val headSize = if (eventLines.size > 1) HEAD_SIZE_COMPACT else HEAD_SIZE
        val head = SkinHead.render(skin, headSize - 2 * OUTLINE)
        val eventHeight = eventLines.size.coerceAtLeast(1).let { it * lineHeight + (it - 1) * LINE_GAP }

        val contentHeight = lineHeight + SECTION_GAP + DIVIDER_HEIGHT + SECTION_GAP +
                eventHeight + HEAD_GAP + headSize + HEAD_GAP +
                lineHeight + SECTION_GAP + DIVIDER_HEIGHT + SECTION_GAP + lineHeight
        var y = INNER_TOP + (INNER_HEIGHT - contentHeight) / 2

        drawCentered(image, TITLE, y, bold = true)
        y += lineHeight + SECTION_GAP
        drawDivider(image, y)
        y += DIVIDER_HEIGHT + SECTION_GAP

        eventLines.forEachIndexed { index, line ->
            drawCentered(image, line, y + index * (lineHeight + LINE_GAP), bold = false)
        }
        y += eventHeight + HEAD_GAP

        drawHead(image, head, y + OUTLINE)
        y += headSize + HEAD_GAP

        drawCentered(image, nameLine, y, bold = false)
        y += lineHeight + SECTION_GAP
        drawDivider(image, y)
        y += DIVIDER_HEIGHT + SECTION_GAP

        drawCentered(image, CertificateDates.format(date), y, bold = false)
        return image
    }

    private fun drawBackground(image: BufferedImage) {
        fill(image, 0, 0, SIZE, SIZE, PARCHMENT)
        strokeRect(image, 0, BORDER)
        strokeRect(image, 1, BORDER)
        strokeRect(image, 3, PARCHMENT_SHADE)
        drawCornerOrnaments(image)
    }

    /** A small bracket in every corner, mirrored from the top left one. */
    private fun drawCornerOrnaments(image: BufferedImage) {
        val topLeft = listOf(5 to 5, 6 to 5, 7 to 5, 8 to 5, 5 to 6, 5 to 7, 5 to 8, 7 to 7)
        for ((x, y) in topLeft) {
            image.setRGB(x, y, INK.rgb)
            image.setRGB(SIZE - 1 - x, y, INK.rgb)
            image.setRGB(x, SIZE - 1 - y, INK.rgb)
            image.setRGB(SIZE - 1 - x, SIZE - 1 - y, INK.rgb)
        }
    }

    /** Two lines with a small diamond in the middle, symmetric around the centre of the map. */
    private fun drawDivider(image: BufferedImage, top: Int) {
        val middle = top + 1
        fill(image, 24, middle, 35, 1, BORDER)
        fill(image, SIZE - 24 - 35, middle, 35, 1, BORDER)

        val center = SIZE / 2
        fill(image, center - 1, top, 2, 1, INK)
        image.setRGB(center - 2, middle, INK.rgb)
        image.setRGB(center + 1, middle, INK.rgb)
        fill(image, center - 1, top + 2, 2, 1, INK)
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

    private fun fill(image: BufferedImage, x: Int, y: Int, width: Int, height: Int, color: Color) {
        for (py in y until y + height) for (px in x until x + width) image.setRGB(px, py, color.rgb)
    }

    private fun strokeRect(image: BufferedImage, inset: Int, color: Color) {
        val size = SIZE - 2 * inset
        fill(image, inset, inset, size, 1, color)
        fill(image, inset, SIZE - 1 - inset, size, 1, color)
        fill(image, inset, inset, 1, size, color)
        fill(image, SIZE - 1 - inset, inset, 1, size, color)
    }
}
