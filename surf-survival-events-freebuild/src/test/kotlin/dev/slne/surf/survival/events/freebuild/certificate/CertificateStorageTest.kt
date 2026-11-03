package dev.slne.surf.survival.events.freebuild.certificate

import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.io.path.deleteExisting
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CertificateStorageTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `saved certificates are loaded again with metadata and image`() {
        val record = record(mapId = 42)
        CertificateStorage(directory).save(record, image(0xFF4C3223.toInt()))

        val storage = CertificateStorage(directory)
        val loaded = storage.loadRecords()

        assertEquals(emptyList(), loaded.failures)
        assertEquals(record, loaded.records.single())
        val image = storage.loadImage(42)
        assertEquals(0xFF4C3223.toInt(), image.getRGB(5, 5))
        assertEquals(128, image.width)
    }

    @Test
    fun `several certificates are loaded independently`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFFFFFFFF.toInt()))

        val loaded = CertificateStorage(directory).loadRecords()

        assertEquals(setOf(1, 2), loaded.records.map { it.mapId }.toSet())
    }

    @Test
    fun `a certificate without image is reported and skipped`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFF000000.toInt()))
        directory.resolve("certificates/2.png").deleteExisting()

        val loaded = CertificateStorage(directory).loadRecords()

        assertEquals(listOf(1), loaded.records.map { it.mapId })
        assertEquals(2, loaded.failures.single().mapId)
        assertTrue("2.png" in loaded.failures.single().reason, loaded.failures.single().reason)
    }

    @Test
    fun `loading a corrupt image fails`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        directory.resolve("certificates/1.png").writeBytes(byteArrayOf(1, 2, 3, 4))

        assertFails { CertificateStorage(directory).loadImage(1) }
    }

    @Test
    fun `loading an image of the wrong size fails`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB))

        assertFails { CertificateStorage(directory).loadImage(1) }
    }

    @Test
    fun `corrupt metadata is reported and skipped`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 2), image(0xFF000000.toInt()))
        directory.resolve("certificates/2.json").writeText("{ not json")

        val loaded = CertificateStorage(directory).loadRecords()

        assertEquals(listOf(1), loaded.records.map { it.mapId })
        assertEquals(LoadFailure("2.json", 2, loaded.failures.single().reason), loaded.failures.single())
    }

    @Test
    fun `metadata stored under the wrong map id is reported`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.save(record(mapId = 3), image(0xFF000000.toInt()))
        directory.resolve("certificates/3.json").writeText(directory.resolve("certificates/1.json").readText())

        val loaded = CertificateStorage(directory).loadRecords()

        assertEquals(listOf(1), loaded.records.map { it.mapId })
        assertEquals(3, loaded.failures.single().mapId)
    }

    @Test
    fun `no temporary files are left behind after saving`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        storage.saveRecord(record(mapId = 1).copy(claimedAt = Instant.parse("2026-11-02T20:00:00Z")))

        assertEquals(
            setOf("1.json", "1.png"),
            directory.resolve("certificates").listDirectoryEntries().map { it.name }.toSet()
        )
    }

    @Test
    fun `a pending certificate is loaded as pending`() {
        CertificateStorage(directory).save(record(mapId = 1), image(0xFF000000.toInt()))

        val loaded = CertificateStorage(directory).loadRecords().records.single()

        assertNull(loaded.claimedAt)
    }

    @Test
    fun `an updated record keeps its image and stores the claim time`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF4C3223.toInt()))
        val claimed = record(mapId = 1).copy(claimedAt = Instant.parse("2026-11-02T20:00:00Z"))

        storage.saveRecord(claimed)

        val reloaded = CertificateStorage(directory)
        assertEquals(claimed, reloaded.loadRecords().records.single())
        assertEquals(0xFF4C3223.toInt(), reloaded.loadImage(1).getRGB(5, 5))
    }

    @Test
    fun `metadata without claim information counts as claimed at creation`() {
        val storage = CertificateStorage(directory)
        storage.save(record(mapId = 1), image(0xFF000000.toInt()))
        directory.resolve("certificates/1.json").writeText(
            """
            {
              "mapId": 1,
              "playerUuid": "069a79f4-44e9-4726-a5be-fca90e38aaf5",
              "playerName": "Notch",
              "eventName": "Winterevent 2026",
              "date": "2026-11-02",
              "createdBy": "853c80ef-3c37-49fd-aa49-938b674adae6",
              "createdAt": "2026-11-02T18:30:00Z"
            }
            """.trimIndent()
        )

        val loaded = CertificateStorage(directory).loadRecords().records.single()

        assertEquals(Instant.parse("2026-11-02T18:30:00Z"), loaded.claimedAt)
    }

    @Test
    fun `an empty data folder loads nothing`() {
        val loaded = CertificateStorage(directory).loadRecords()

        assertEquals(emptyList(), loaded.records)
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
