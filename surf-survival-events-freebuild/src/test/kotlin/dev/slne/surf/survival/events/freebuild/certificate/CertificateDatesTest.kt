package dev.slne.surf.survival.events.freebuild.certificate

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CertificateDatesTest {

    @Test
    fun `parses a valid german date`() {
        assertEquals(LocalDate.of(2026, 11, 2), CertificateDates.parse("02.11.2026"))
    }

    @Test
    fun `rejects a date that does not exist instead of clamping it`() {
        assertNull(CertificateDates.parse("31.02.2026"))
    }

    @Test
    fun `rejects single digit day and month`() {
        assertNull(CertificateDates.parse("2.11.2026"))
    }

    @Test
    fun `rejects iso formatted dates`() {
        assertNull(CertificateDates.parse("2026-11-02"))
    }

    @Test
    fun `formats dates with leading zeros`() {
        assertEquals("02.03.2026", CertificateDates.format(LocalDate.of(2026, 3, 2)))
    }
}
