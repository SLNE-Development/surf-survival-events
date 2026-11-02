package dev.slne.surf.survival.events.freebuild.certificate

import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CertificateStorageTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `saved certificates are loaded again with image and metadata`() {
        val record = record(mapId = 42)
        CertificateStorage(directory).save(record, image(0xFF4C3223.toInt()))

        val loaded = CertificateStorage(directory).loadAll()

        assertEquals(emptyList(), loaded.failures)
        val certificate = loaded.certificates.single()
        assertEquals(record, certificate.record)
        assertEquals(0xFF4C3223.toInt(), certificate.image.getRGB(5, 5))
        assertEquals(128, certificate.image.width)
    }

    @Test
    fun `several certificates are loaded independently`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFFFFFFFF.toInt()))

        val loaded = CertificateStorage(directory).loadAll()

        assertEquals(setOf(1, 2), loaded.certificates.map { it.record.mapId }.toSet())
    }

    @Test
    fun `a missing image is reported and skipped`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFF000000.toInt()))
        directory.resolve("certificates/2.png").deleteExisting()

        val loaded = CertificateStorage(directory).loadAll()

        assertEquals(listOf(1), loaded.certificates.map { it.record.mapId })
        assertEquals(1, loaded.failures.size)
        assertTrue("2" in loaded.failures.single(), loaded.failures.single())
    }

    @Test
    fun `a corrupt image is reported and skipped`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFF000000.toInt()))
        directory.resolve("certificates/2.png").writeBytes(byteArrayOf(1, 2, 3, 4))

        val loaded = CertificateStorage(directory).loadAll()

        assertEquals(listOf(1), loaded.certificates.map { it.record.mapId })
        assertEquals(1, loaded.failures.size)
    }

    @Test
    fun `corrupt metadata is reported and skipped`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFF000000.toInt()))
        directory.resolve("certificates/2.json").writeText("{ not json")

        val loaded = CertificateStorage(directory).loadAll()

        assertEquals(listOf(1), loaded.certificates.map { it.record.mapId })
        assertEquals(1, loaded.failures.size)
    }

    @Test
    fun `an empty data folder loads nothing`() {
        val loaded = CertificateStorage(directory).loadAll()

        assertEquals(emptyList(), loaded.certificates)
        assertEquals(emptyList(), loaded.failures)
    }

    private fun record(mapId: Int) = CertificateRecord(
        mapId = mapId,
        playerUuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"),
        playerName = "Notch",
        eventName = "Winterevent 2026",
        date = LocalDate.of(2026, 11, 2),
        createdBy = UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"),
        createdAt = Instant.parse("2026-11-02T18:30:00Z"),
    )

    private fun image(color: Int) = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB).apply {
        for (x in 0 until 128) for (y in 0 until 128) setRGB(x, y, color)
    }
}
