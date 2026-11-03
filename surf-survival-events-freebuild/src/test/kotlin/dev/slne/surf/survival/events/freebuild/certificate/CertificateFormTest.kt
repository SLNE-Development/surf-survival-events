package dev.slne.surf.survival.events.freebuild.certificate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CertificateFormTest {
    private fun form(
        eventName: String? = "Winterevent 2026",
        date: String? = "02.11.2026",
        server: String? = "Survival Events",
        minutes: Float? = 10f,
    ) = CertificateForm.from(eventName, date, server, minutes)

    @Test
    fun `normalizes the entered values`() {
        val form = form(eventName = "  Winter   event  ", date = " 02.11.2026 ", server = " Events ", minutes = 9.6f)

        assertEquals(CertificateForm("Winter event", "02.11.2026", "Events", 10), form)
    }

    @Test
    fun `treats missing values as empty`() {
        assertEquals(CertificateForm("", "", "", 0), form(null, null, null, null))
    }

    @Test
    fun `accepts a complete form`() {
        assertNull(form().validate())
    }

    @Test
    fun `rejects an empty event name`() {
        assertNotNull(form(eventName = "   ").validate())
    }

    @Test
    fun `rejects an event name that is too long`() {
        assertNotNull(form(eventName = "x".repeat(CertificateForm.MAX_EVENT_NAME_LENGTH + 1)).validate())
        assertNull(form(eventName = "x".repeat(CertificateForm.MAX_EVENT_NAME_LENGTH)).validate())
    }

    @Test
    fun `accepts german event names`() {
        assertNull(form(eventName = "Straßenfest – Größte Übung").validate())
    }

    @Test
    fun `rejects an event name the certificate cannot show`() {
        val error = form(eventName = "Café Fest").validate()

        assertNotNull(error)
        assertTrue("é" in error, error)
    }

    @Test
    fun `rejects an invalid date`() {
        assertNotNull(form(date = "31.02.2026").validate())
        assertNotNull(form(date = "2026-11-02").validate())
    }

    @Test
    fun `rejects an empty server`() {
        assertNotNull(form(server = " ").validate())
    }

    @Test
    fun `normalizes event names the same way everywhere`() {
        assertEquals("Das grosse Fest", CertificateForm.normalizeEventName("\tDas  grosse\nFest "))
    }
}
