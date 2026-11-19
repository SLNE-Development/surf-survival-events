package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.map.MapFont.CharacterSprite
import org.bukkit.map.MinecraftFont
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Draws text with the bitmap glyphs of Bukkit's built-in [MinecraftFont], pixel by pixel and without anti-aliasing.
 * Bold text is drawn like the Minecraft client does it: every glyph a second time, shifted by one pixel.
 *
 * [MinecraftFont] assigns the code page 437 symbols to the code points U+00B0 to U+00FF and thereby overwrites its
 * own glyphs of the accented letters, so `ä` would be drawn as `Σ` and `°` as `░`. It also draws the apostrophe with
 * the glyph of the grave accent. The German umlauts and the apostrophe therefore come from a corrected table, `ß` and
 * typographic punctuation are written in their plain form, and the other characters of that range count as not
 * drawable.
 */
object PixelFont {
    private const val REPLACEMENT = '?'
    private const val SECTION_SIGN = '§'
    private const val DELETE = '\u007F'
    private val font = MinecraftFont.Font
    private val misassignedRange = '\u00B0'..'\u00FF'

    private val corrected = mapOf(
        '\'' to glyph(2, 2, 1, 0, 0, 0, 0, 0),
        'ä' to glyph(10, 0, 14, 16, 30, 17, 30, 0),
        'ö' to glyph(10, 0, 14, 17, 17, 17, 14, 0),
        'ü' to glyph(10, 0, 17, 17, 17, 17, 30, 0),
        'Ä' to glyph(17, 14, 17, 31, 17, 17, 17, 0),
        'Ö' to glyph(17, 14, 17, 17, 17, 17, 14, 0),
        'Ü' to glyph(17, 0, 17, 17, 17, 17, 14, 0),
    )

    private val plainForms = mapOf(
        'ß' to "ss",
        'ẞ' to "SS",
        '–' to "-",
        '—' to "-",
        '„' to "\"",
        '“' to "\"",
        '”' to "\"",
        '‚' to "'",
        '‘' to "'",
        '’' to "'",
        '…' to "...",
    )

    val lineHeight: Int get() = font.height

    /** Writes characters with a plain form in that form and replaces all other characters that cannot be drawn. */
    fun sanitize(text: String): String = buildString(text.length) {
        for (char in text) {
            val plainForm = plainForms[char]
            when {
                plainForm != null -> append(plainForm)
                sprite(char) != null -> append(char)
                else -> append(REPLACEMENT)
            }
        }
    }

    /** The characters of [text] that can neither be drawn nor written in a plain form. */
    fun undrawable(text: String): Set<Char> =
        text.asIterable().filterTo(LinkedHashSet()) { it !in plainForms && sprite(it) == null }

    fun width(text: String, bold: Boolean): Int {
        val safe = sanitize(text)
        if (safe.isEmpty()) return 0

        val glyphs = safe.sumOf { sprite(it)!!.width }
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
            val sprite = sprite(char)!!
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

    private fun sprite(char: Char): CharacterSprite? = when {
        char == SECTION_SIGN || char == DELETE || char < ' ' -> null
        char in corrected -> corrected[char]
        char in misassignedRange -> null
        else -> font.getChar(char)
    }

    /** Builds a glyph like [MinecraftFont] does: bit `n` of a row is column `n`, the width ends at the last ink. */
    private fun glyph(vararg rows: Int): CharacterSprite {
        val width = rows.maxOf { Int.SIZE_BITS - it.countLeadingZeroBits() }
        val data = BooleanArray(width * rows.size) { (rows[it / width] shr (it % width)) and 1 == 1 }
        return CharacterSprite(width, rows.size, data)
    }

    private fun BufferedImage.setPixelSafe(x: Int, y: Int, rgb: Int) {
        if (x in 0 until width && y in 0 until height) setRGB(x, y, rgb)
    }
}
