package dev.slne.surf.survival.events.freebuild.certificate

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticipantsTest {
    private val first = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val second = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val third = UUID.fromString("00000000-0000-0000-0000-000000000003")

    @Test
    fun `keeps players with enough playtime sorted by playtime`() {
        val selection = Participants.select(
            mapOf(first to 600L, second to 3600L, third to 599L),
            minimumMinutes = 10
        ) { false }

        assertEquals(
            listOf(Participant(second, 3600), Participant(first, 600)),
            selection.eligible
        )
    }

    @Test
    fun `keeps everyone with playtime when no minimum is set`() {
        val selection = Participants.select(mapOf(first to 1L, second to 0L), minimumMinutes = 0) { false }

        assertEquals(listOf(Participant(first, 1)), selection.eligible)
    }

    @Test
    fun `skips players that already have a certificate`() {
        val selection = Participants.select(
            mapOf(first to 600L, second to 3600L),
            minimumMinutes = 0
        ) { it == second }

        assertEquals(listOf(Participant(first, 600)), selection.eligible)
        assertEquals(listOf(Participant(second, 3600)), selection.alreadyCertified)
    }

    @Test
    fun `players without enough playtime are not counted as already certified`() {
        val selection = Participants.select(mapOf(first to 60L), minimumMinutes = 10) { true }

        assertEquals(emptyList(), selection.eligible)
        assertEquals(emptyList(), selection.alreadyCertified)
    }
}
