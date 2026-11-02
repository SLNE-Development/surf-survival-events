package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.map.MinecraftFont
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Draws text with the bitmap glyphs of Bukkit's built-in [MinecraftFont], pixel by pixel and without anti-aliasing.
 * Bold text is drawn like the Minecraft client does it: every glyph a second time, shifted by one pixel.
 */
object PixelFont {
    private const val REPLACEMENT = '?'
    private const val SECTION_SIGN = '§'
    private val font = MinecraftFont.Font

    val lineHeight: Int get() = font.height

    fun sanitize(text: String): String = buildString(text.length) {
        for (char in text) append(if (isDrawable(char)) char else REPLACEMENT)
    }

    fun width(text: String, bold: Boolean): Int {
        val safe = sanitize(text)
        if (safe.isEmpty()) return 0

        val glyphs = safe.sumOf { font.getChar(it)!!.width }
        val spacing = safe.length - 1
        val boldExtra = if (bold) safe.length else 0
        return glyphs + spacing + boldExtra
    }

    fun wrap(text: String, maxWidth: Int, maxLines: Int, bold: Boolean): List<String> =
        TextWrap.wrap(sanitize(text), maxWidth, maxLines) { width(it, bold) }

    fun draw(image: BufferedImage, x: Int, y: Int, text: String, color: Color, bold: Boolean) {
        val rgb = color.rgb
        var cursor = x

        for (char in sanitize(text)) {
            val sprite = font.getChar(char)!!
            for (row in 0 until sprite.height) {
                for (col in 0 until sprite.width) {
                    if (!sprite.get(row, col)) continue
                    image.setPixelSafe(cursor + col, y + row, rgb)
                    if (bold) image.setPixelSafe(cursor + col + 1, y + row, rgb)
                }
            }
            cursor += sprite.width + 1 + if (bold) 1 else 0
        }
    }

    private fun isDrawable(char: Char) = char != SECTION_SIGN && font.getChar(char) != null

    private fun BufferedImage.setPixelSafe(x: Int, y: Int, rgb: Int) {
        if (x in 0 until width && y in 0 until height) setRGB(x, y, rgb)
    }
}
