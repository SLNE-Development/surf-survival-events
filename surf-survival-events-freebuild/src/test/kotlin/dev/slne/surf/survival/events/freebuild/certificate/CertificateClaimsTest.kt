package dev.slne.surf.survival.events.freebuild.certificate

import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class CertificateClaimsTest {
    private val notch = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5")
    private val jeb = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6")
    private val now = Instant.parse("2026-11-02T20:00:00Z")

    private val registry = CertificateRegistry()
    private val steps = mutableListOf<String>()
    private val persisted = mutableListOf<CertificateRecord>()
    private val claims = CertificateClaims(
        registry = registry,
        persist = { steps += "persist"; persisted += it },
        clock = { now },
    )

    private val pending = CertificateRecord(
        mapId = 1,
        playerUuid = notch,
        playerName = "Notch",
        eventName = "Winterevent 2026",
        date = LocalDate.of(2026, 11, 2),
        createdBy = jeb,
        createdAt = Instant.parse("2026-11-02T18:00:00Z"),
    )

    private fun claim(player: UUID = notch, fits: Boolean = true) = claims.claim(
        playerUuid = player,
        mapId = 1,
        handOver = { steps += "handOver"; fits },
        savePlayer = { steps += "savePlayer" },
    )

    @Test
    fun `hands the certificate over and saves the player before persisting the claim`() {
        registry.add(pending)

        val result = claim()

        assertIs<ClaimResult.Claimed>(result)
        assertEquals(now, result.record.claimedAt)
        assertEquals(listOf("handOver", "savePlayer", "persist"), steps)
        assertEquals(listOf(result.record), persisted)
        assertEquals(emptyList(), registry.pendingFor(notch))
    }

    @Test
    fun `a full inventory keeps the certificate pending and persists nothing`() {
        registry.add(pending)

        assertIs<ClaimResult.InventoryFull>(claim(fits = false))

        assertEquals(listOf("handOver"), steps)
        assertEquals(emptyList(), persisted)
        assertNull(registry[1]?.claimedAt)
    }

    @Test
    fun `can be claimed after the inventory was full`() {
        registry.add(pending)
        claim(fits = false)

        assertIs<ClaimResult.Claimed>(claim())
    }

    @Test
    fun `a failing hand over does not block the certificate`() {
        registry.add(pending)

        assertFailsWith<IllegalStateException> {
            claims.claim(notch, 1, handOver = { error("inventory broke") }, savePlayer = {})
        }

        assertIs<ClaimResult.Claimed>(claim())
    }

    @Test
    fun `a claimed certificate is not handed out again`() {
        registry.add(pending)
        claim()
        steps.clear()

        assertIs<ClaimResult.AlreadyClaimed>(claim())
        assertEquals(emptyList(), steps)
    }

    @Test
    fun `nobody else can claim the certificate`() {
        registry.add(pending)

        assertIs<ClaimResult.NotFound>(claim(player = jeb))
        assertEquals(emptyList(), steps)
    }

    @Test
    fun `a certificate offered again can be claimed again`() {
        registry.add(pending.copy(claimedAt = Instant.parse("2026-11-02T19:00:00Z")))
        registry.add(pending)

        assertIs<ClaimResult.Claimed>(claim())
    }
}
