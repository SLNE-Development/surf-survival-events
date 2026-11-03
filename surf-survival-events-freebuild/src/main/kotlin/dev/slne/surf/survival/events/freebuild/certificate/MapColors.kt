package dev.slne.surf.survival.events.freebuild.certificate

import kotlin.math.cbrt
import kotlin.math.pow

/**
 * The colours a map can show: the 61 base colours of Minecraft's `MapColor` in four brightnesses.
 *
 * Other colours are matched to the closest map colour in the CIELAB colour space, which follows human perception more
 * closely than the weighted RGB distance Bukkit uses. Skin tones therefore stay beige instead of tipping to yellow,
 * and dark purple stays purple instead of turning black.
 */
object MapColors {
    private val baseColors = intArrayOf(
        0x7FB238, 0xF7E9A3, 0xC7C7C7, 0xFF0000, 0xA0A0FF, 0xA7A7A7, 0x007C00, 0xFFFFFF,
        0xA4A8B8, 0x976D4D, 0x707070, 0x4040FF, 0x8F7748, 0xFFFCF5, 0xD87F33, 0xB24CD8,
        0x6699D8, 0xE5E533, 0x7FCC19, 0xF27FA5, 0x4C4C4C, 0x999999, 0x4C7F99, 0x7F3FB2,
        0x334CB2, 0x664C33, 0x667F33, 0x993333, 0x191919, 0xFAEE4D, 0x5CDBD5, 0x4A80FF,
        0x00D93A, 0x815631, 0x700200, 0xD1B1A1, 0x9F5224, 0x95576C, 0x706C8A, 0xBA8524,
        0x677535, 0xA04D4E, 0x392923, 0x876B62, 0x575C5C, 0x7A4958, 0x4C3E5C, 0x4C3223,
        0x4C522A, 0x8E3C2E, 0x251610, 0xBD3031, 0x943F61, 0x5C191D, 0x167E86, 0x3A8E8C,
        0x562C3E, 0x14B485, 0x646464, 0xD8AF93, 0x7FA796,
    )
    private val brightnesses = intArrayOf(180, 220, 255, 135)

    /** All map colours as RGB without alpha, in the order of their palette index, which starts at 4. */
    val all: List<Int> = baseColors.flatMap { base -> brightnesses.map { shade(base, it) } }

    private val labs = all.associateWith(::lab)

    /** The map colour closest to [rgb], whose alpha is ignored, that is not one of [excluded]. */
    fun nearest(rgb: Int, excluded: Set<Int> = emptySet()): Int {
        val target = lab(rgb and 0xFFFFFF)
        return all.asSequence()
            .filter { it !in excluded }
            .minBy { distanceSquared(target, labs.getValue(it)) }
    }

    private fun shade(rgb: Int, brightness: Int): Int {
        val red = (rgb shr 16 and 0xFF) * brightness / 255
        val green = (rgb shr 8 and 0xFF) * brightness / 255
        val blue = (rgb and 0xFF) * brightness / 255
        return red shl 16 or (green shl 8) or blue
    }

    private fun distanceSquared(first: DoubleArray, second: DoubleArray): Double =
        first.indices.sumOf { (first[it] - second[it]).pow(2) }

    /** Converts an sRGB colour to CIELAB with the D65 white point. */
    private fun lab(rgb: Int): DoubleArray {
        val red = linear(rgb shr 16 and 0xFF)
        val green = linear(rgb shr 8 and 0xFF)
        val blue = linear(rgb and 0xFF)

        val x = (0.4124564 * red + 0.3575761 * green + 0.1804375 * blue) / 0.95047
        val y = 0.2126729 * red + 0.7151522 * green + 0.0721750 * blue
        val z = (0.0193339 * red + 0.1191920 * green + 0.9503041 * blue) / 1.08883

        val fx = labCurve(x)
        val fy = labCurve(y)
        val fz = labCurve(z)
        return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    private fun linear(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    private fun labCurve(value: Double): Double =
        if (value > 216.0 / 24389) cbrt(value) else (24389.0 / 27 * value + 16) / 116
}
