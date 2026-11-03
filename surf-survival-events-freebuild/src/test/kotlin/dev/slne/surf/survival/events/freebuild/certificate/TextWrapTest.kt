package dev.slne.surf.survival.events.freebuild.certificate

import kotlin.test.Test
import kotlin.test.assertEquals

class TextWrapTest {
    /** Every character is 6 px wide, so 66 px fit exactly 11 characters. */
    private val monospace: (String) -> Int = { it.length * 6 }

    @Test
    fun `keeps text that fits on a single line`() {
        assertEquals(listOf("Winter"), TextWrap.wrap("Winter", 66, 2, monospace))
    }

    @Test
    fun `breaks at word boundaries when the text is too wide`() {
        assertEquals(
            listOf("Winterevent", "2026"),
            TextWrap.wrap("Winterevent 2026", 66, 2, monospace)
        )
    }

    @Test
    fun `truncates the last allowed line with an ellipsis`() {
        assertEquals(
            listOf("Das grosse", "Sommerfe..."),
            TextWrap.wrap("Das grosse Sommerfest der Bauprofis", 66, 2, monospace)
        )
    }

    @Test
    fun `truncates a single word that is wider than a line`() {
        assertEquals(
            listOf("Supercal..."),
            TextWrap.wrap("Supercalifragilistic", 66, 1, monospace)
        )
    }

    @Test
    fun `collapses repeated whitespace between words`() {
        assertEquals(listOf("Winter 2026"), TextWrap.wrap("  Winter    2026 ", 66, 2, monospace))
    }
}
