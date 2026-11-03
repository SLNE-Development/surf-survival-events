package dev.slne.surf.survival.events.freebuild.certificate

import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CertificateRegistryTest {
    private val notch = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")
    private val jeb = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6")
    private val date = LocalDate.of(2026, 11, 2)

    private fun record(
        mapId: Int,
        player: UUID = notch,
        eventName: String = "Winterevent 2026",
        date: LocalDate = this.date,
        createdAt: String = "2026-11-02T18:00:00Z",
        claimedAt: Instant? = null,
    ) = CertificateRecord(
        mapId = mapId,
        playerUuid = player,
        playerName = "Player",
        eventName = eventName,
        date = date,
        createdBy = jeb,
        createdAt = Instant.parse(createdAt),
        claimedAt = claimedAt,
    )

    @Test
    fun `lists the pending certificates of a player oldest first`() {
        val registry = CertificateRegistry()
        registry.add(record(1, createdAt = "2026-11-02T19:00:00Z"))
        registry.add(record(2, createdAt = "2026-11-02T18:00:00Z"))
        registry.add(record(3, player = jeb))
        registry.add(record(4, claimedAt = Instant.parse("2026-11-02T20:00:00Z")))

        assertEquals(listOf(2, 1), registry.pendingFor(notch).map { it.mapId })
    }

    @Test
    fun `knows which maps are certificates`() {
        val registry = CertificateRegistry()
        registry.add(record(7))

        assertTrue(7 in registry)
        assertFalse(8 in registry)
        assertEquals(7, registry[7]?.mapId)
        assertNull(registry[8])
    }

    @Test
    fun `replacing a record keeps one entry per certificate`() {
        val registry = CertificateRegistry()
        registry.add(record(1))
        registry.add(record(1, claimedAt = Instant.parse("2026-11-02T20:00:00Z")))

        assertEquals(emptyList(), registry.pendingFor(notch))
        assertTrue(registry.hasCertificate(notch, "Winterevent 2026", date))
    }

    @Test
    fun `knows whether a player already has a certificate for an event`() {
        val registry = CertificateRegistry()
        registry.add(record(1, claimedAt = Instant.parse("2026-11-02T20:00:00Z")))

        assertTrue(registry.hasCertificate(notch, "Winterevent 2026", date))
        assertTrue(registry.hasCertificate(notch, "winterevent 2026", date))
        assertFalse(registry.hasCertificate(notch, "Winterevent 2026", date.plusDays(1)))
        assertFalse(registry.hasCertificate(notch, "Sommerfest", date))
        assertFalse(registry.hasCertificate(jeb, "Winterevent 2026", date))
    }

    @Test
    fun `reserves a certificate nobody has yet`() {
        assertIs<Reservation.Reserved>(CertificateRegistry().reserve(notch, "Winterevent 2026", date))
    }

    @Test
    fun `refuses a second reservation while the first one is being issued`() {
        val registry = CertificateRegistry()
        registry.reserve(notch, "Winterevent 2026", date)

        assertIs<Reservation.InProgress>(registry.reserve(notch, "WINTEREVENT 2026", date))
        assertIs<Reservation.Reserved>(registry.reserve(notch, "Winterevent 2026", date.plusDays(1)))
        assertIs<Reservation.Reserved>(registry.reserve(jeb, "Winterevent 2026", date))
    }

    @Test
    fun `reports the existing certificate instead of reserving a second one`() {
        val registry = CertificateRegistry()
        registry.add(record(1, claimedAt = Instant.parse("2026-11-02T20:00:00Z")))

        val reservation = registry.reserve(notch, "winterevent 2026", date)

        assertIs<Reservation.Exists>(reservation)
        assertEquals(1, reservation.record.mapId)
    }

    @Test
    fun `adding the issued certificate ends its reservation`() {
        val registry = CertificateRegistry()
        registry.reserve(notch, "Winterevent 2026", date)

        registry.add(record(1))

        assertIs<Reservation.Exists>(registry.reserve(notch, "Winterevent 2026", date))
    }

    @Test
    fun `a released reservation can be taken again`() {
        val registry = CertificateRegistry()
        registry.reserve(notch, "Winterevent 2026", date)

        registry.release(notch, "winterevent 2026", date)

        assertIs<Reservation.Reserved>(registry.reserve(notch, "Winterevent 2026", date))
    }

    @Test
    fun `unreadable certificates still count as certificates without a record`() {
        val registry = CertificateRegistry()
        registry.addUnreadable(9)

        assertTrue(9 in registry)
        assertNull(registry[9])
        assertEquals(emptyList(), registry.pendingFor(notch))
    }

    @Test
    fun `knows the highest map id of all readable certificates`() {
        val registry = CertificateRegistry()
        assertNull(registry.highestMapId())

        registry.add(record(4))
        registry.add(record(2))
        registry.addUnreadable(2_147_483_647)

        assertEquals(4, registry.highestMapId())
    }

    @Test
    fun `reopening a claimed certificate makes it pending again`() {
        val registry = CertificateRegistry()
        registry.add(record(1, claimedAt = Instant.parse("2026-11-02T20:00:00Z")))

        assertTrue(registry.reopen(1))

        assertNull(registry[1]?.claimedAt)
        assertEquals(listOf(1), registry.pendingFor(notch).map { it.mapId })
    }

    @Test
    fun `reopening a pending or unknown certificate changes nothing`() {
        val registry = CertificateRegistry()
        val pending = record(1)
        registry.add(pending)

        assertFalse(registry.reopen(1))
        assertFalse(registry.reopen(2))
        assertEquals(pending, registry[1])
    }

    @Test
    fun `starts a claim for the owner of a pending certificate`() {
        val registry = CertificateRegistry()
        registry.add(record(1))

        val start = registry.beginClaim(1, notch)

        assertIs<ClaimStart.Started>(start)
        assertEquals(1, start.record.mapId)
    }

    @Test
    fun `does not let another player claim a certificate`() {
        val registry = CertificateRegistry()
        registry.add(record(1))

        assertIs<ClaimStart.NotFound>(registry.beginClaim(1, jeb))
    }

    @Test
    fun `reports an unknown certificate as not found`() {
        assertIs<ClaimStart.NotFound>(CertificateRegistry().beginClaim(1, notch))
    }

    @Test
    fun `refuses a second claim while the first one is still running`() {
        val registry = CertificateRegistry()
        registry.add(record(1))
        registry.beginClaim(1, notch)

        assertIs<ClaimStart.InProgress>(registry.beginClaim(1, notch))
    }

    @Test
    fun `allows a new claim after an aborted one`() {
        val registry = CertificateRegistry()
        registry.add(record(1))
        registry.beginClaim(1, notch)

        registry.abortClaim(1)

        assertIs<ClaimStart.Started>(registry.beginClaim(1, notch))
    }

    @Test
    fun `a finished claim marks the certificate as claimed`() {
        val registry = CertificateRegistry()
        registry.add(record(1))
        registry.beginClaim(1, notch)

        val claimed = registry.finishClaim(1, Instant.parse("2026-11-02T20:00:00Z"))

        assertEquals(Instant.parse("2026-11-02T20:00:00Z"), claimed.claimedAt)
        assertEquals(emptyList(), registry.pendingFor(notch))
        assertIs<ClaimStart.AlreadyClaimed>(registry.beginClaim(1, notch))
    }
}
