package dev.slne.surf.survival.events.freebuild.certificate

import io.mockk.every
import io.mockk.mockk
import org.bukkit.map.MapView
import java.time.Instant
import java.time.LocalDate
import java.util.*
import java.util.logging.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CertificateMapsTest {
    private val registry = CertificateRegistry()
    private val attached = mutableListOf<Int>()
    private val created = mutableListOf<Int>()
    private var nextId = 0

    private val maps = CertificateMaps(
        registry = registry,
        createMap = { map(nextId++).also { created += it.id } },
        attach = { attached += it.id },
        logger = Logger.getAnonymousLogger(),
    )

    private fun map(id: Int, locked: Boolean = false) = mockk<MapView> {
        every { getId() } returns id
        every { isLocked } returns locked
    }

    private fun certificates(vararg mapIds: Int) = mapIds.forEach { mapId ->
        registry.add(
            CertificateRecord(
                mapId = mapId,
                playerUuid = UUID.randomUUID(),
                playerName = "Player",
                eventName = "Winterevent 2026",
                date = LocalDate.of(2026, 11, 2),
                createdBy = UUID.randomUUID(),
                createdAt = Instant.parse("2026-11-02T18:00:00Z"),
            )
        )
    }

    @Test
    fun `a loaded certificate map gets the certificate renderer`() {
        certificates(3)

        maps.onInitialize(map(3, locked = true))
        maps.onInitialize(map(4, locked = true))

        assertEquals(listOf(3), attached)
    }

    @Test
    fun `a new map with the id of a certificate shows the certificate instead of replacing it`() {
        certificates(3)

        maps.onInitialize(map(3, locked = false))

        assertEquals(listOf(3), attached)
    }

    @Test
    fun `a new certificate never gets the id of an existing one`() {
        certificates(0, 1, 3)

        val map = maps.createUnused()

        assertEquals(2, map.id)
        assertEquals(listOf(0, 1, 2), created)
        assertEquals(listOf(0, 1, 2), attached)
    }

    @Test
    fun `nothing is repaired without certificates`() {
        assertIs<MapRepair.NotNeeded>(maps.repair())
        assertEquals(emptyList(), created)
    }

    @Test
    fun `world map ids ahead of the certificates need no repair`() {
        certificates(3)
        nextId = 4

        assertIs<MapRepair.NotNeeded>(maps.repair())

        assertEquals(listOf(4), created)
        assertEquals(emptyList(), attached)
    }

    @Test
    fun `map ids behind the certificates are caught up and the certificates restored`() {
        certificates(5, 7, 8)
        nextId = 4

        val repair = maps.repair()

        assertEquals(MapRepair.Restored(3), repair)
        assertEquals(listOf(4, 5, 6, 7, 8), created)
        assertEquals(listOf(5, 7, 8), attached)
    }

    @Test
    fun `a gap that is too large is only reported`() {
        certificates(2, CertificateMaps.MAX_REPAIR_GAP + 10)
        nextId = 2

        val repair = maps.repair()

        assertEquals(MapRepair.TooFarBehind(2, CertificateMaps.MAX_REPAIR_GAP + 10), repair)
        assertEquals(listOf(2), created)
        assertEquals(listOf(2), attached)
    }

    @Test
    fun `an unreadable file name does not drive the repair`() {
        certificates(3)
        registry.addUnreadable(2_147_483_647)
        nextId = 4

        assertIs<MapRepair.NotNeeded>(maps.repair())
        assertEquals(listOf(4), created)
    }
}
