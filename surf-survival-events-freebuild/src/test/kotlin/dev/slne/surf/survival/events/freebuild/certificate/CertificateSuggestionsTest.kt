package dev.slne.surf.survival.events.freebuild.certificate

import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CertificateSuggestionsTest {
    private val notch = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")
    private val jeb = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6")
    private val today = LocalDate.of(2026, 11, 3)

    private fun record(mapId: Int, player: UUID, name: String, eventName: String, date: LocalDate) = CertificateRecord(
        mapId = mapId,
        playerUuid = player,
        playerName = name,
        eventName = eventName,
        date = date,
        createdBy = jeb,
        createdAt = Instant.parse("2026-11-02T18:00:00Z"),
    )

    private val newestFirst = listOf(
        record(3, notch, "Notch", "Döner-Wettfressen", LocalDate.of(2026, 11, 2)),
        record(2, jeb, "jeb_", "döner-wettfressen", LocalDate.of(2026, 11, 2)),
        record(1, notch, "Notch", "Bauevent", LocalDate.of(2026, 10, 5)),
    )

    @Test
    fun `offers today first and then the days of earlier events`() {
        assertEquals(listOf("03.11.2026", "02.11.2026", "05.10.2026"), CertificateSuggestions.dates(newestFirst, today))
    }

    @Test
    fun `does not offer today twice`() {
        assertEquals(listOf("02.11.2026", "05.10.2026"), CertificateSuggestions.dates(newestFirst, LocalDate.of(2026, 11, 2)))
    }

    @Test
    fun `offers every event name once, newest first`() {
        assertEquals(listOf("Döner-Wettfressen", "Bauevent"), CertificateSuggestions.eventNames(newestFirst))
    }

    @Test
    fun `offers only the events of the given player, whatever the case of the name`() {
        assertEquals(listOf("Döner-Wettfressen", "Bauevent"), CertificateSuggestions.eventNamesOf(newestFirst, "notch"))
        assertEquals(listOf("döner-wettfressen"), CertificateSuggestions.eventNamesOf(newestFirst, "JEB_"))
        assertEquals(emptyList(), CertificateSuggestions.eventNamesOf(newestFirst, "Dinnerbone"))
    }

    @Test
    fun `finds the certificates of an event regardless of case and spacing`() {
        val ofNotch = newestFirst.filter { it.playerUuid == notch }

        assertEquals(listOf(3), CertificateSuggestions.forEvent(ofNotch, "  döner-WETTFRESSEN ").map { it.mapId })
        assertEquals(emptyList(), CertificateSuggestions.forEvent(ofNotch, "Sommerfest"))
    }
}
