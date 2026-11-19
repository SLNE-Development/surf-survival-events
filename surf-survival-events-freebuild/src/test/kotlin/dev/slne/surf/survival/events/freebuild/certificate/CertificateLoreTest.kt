package dev.slne.surf.survival.events.freebuild.certificate

import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CertificateLoreTest {
    private val record = CertificateRecord(
        mapId = 1,
        playerUuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        playerName = "Notch",
        eventName = "Winterevent 2026",
        date = LocalDate.of(2026, 11, 2),
        createdBy = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"),
        createdAt = Instant.parse("2026-11-02T18:30:00Z"),
    )

    @Test
    fun `describes owner, date and copy protection`() {
        val lines = CertificateLore.lines(record).map(PlainTextComponentSerializer.plainText()::serialize)

        assertEquals(
            listOf(
                "",
                "Ausgestellt für: Notch",
                "Teilgenommen am: 02.11.2026",
                "",
                "Persönliche, kopiergeschützte Urkunde"
            ),
            lines
        )
    }

    @Test
    fun `lore lines are not italic`() {
        assertTrue(CertificateLore.lines(record).all { it.decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE })
    }
}
