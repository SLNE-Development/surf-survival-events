package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.map.MapPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapColorsTest {
    @Suppress("DEPRECATION", "removal")
    @Test
    fun `knows exactly the colours of the map palette`() {
        val bukkit = (4 until 248).map { MapPalette.getColor(it.toByte()).rgb and 0xFFFFFF }

        assertEquals(bukkit, MapColors.all)
    }

    @Test
    fun `map colours are matched to themselves`() {
        for (color in MapColors.all) {
            assertEquals(color, MapColors.nearest(color))
        }
    }

    @Test
    fun `ignores the alpha channel`() {
        assertEquals(0xD8AF93, MapColors.nearest(0xFFD8AF93.toInt()))
    }

    @Test
    fun `never picks an excluded colour`() {
        val excluded = setOf(0xF7E9A3, 0xD5C98C)

        assertTrue(MapColors.nearest(0xF7E9A3, excluded) !in excluded)
        assertTrue(MapColors.nearest(0xF8CF88, excluded) !in excluded)
    }

    @Test
    fun `peach skin next to the paper stays beige instead of tipping to gold`() {
        assertEquals(0xD8AF93, MapColors.nearest(0xFFD081, excluded = setOf(0xF7E9A3, 0xD5C98C)))
    }

    @Test
    fun `dark purple stays purple`() {
        assertEquals(0x43215E, MapColors.nearest(0x3C1450))
    }
}
