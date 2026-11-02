package dev.slne.surf.survival.events.freebuild.certificate

import com.google.gson.GsonBuilder
import java.awt.image.BufferedImage
import java.io.OutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.*

data class CertificateRecord(
    val mapId: Int,
    val playerUuid: UUID,
    val playerName: String,
    val eventName: String,
    val date: LocalDate,
    val createdBy: UUID,
    val createdAt: Instant,
)

class StoredCertificate(val record: CertificateRecord, val image: BufferedImage)

data class LoadedCertificates(val certificates: List<StoredCertificate>, val failures: List<String>)

/**
 * Stores every certificate as `certificates/<mapId>.png` plus `certificates/<mapId>.json` below the plugin folder.
 * The metadata file is written last, so a certificate only counts as stored once both files exist.
 */
class CertificateStorage(dataDirectory: Path) {
    private val directory = dataDirectory.resolve("certificates")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun save(record: CertificateRecord, image: BufferedImage) {
        directory.createDirectories()
        writeAtomically(directory.resolve("${record.mapId}.png")) { output ->
            check(ImageIO.write(image, "png", output)) { "No PNG writer available" }
        }
        writeAtomically(directory.resolve("${record.mapId}.json")) { output ->
            output.write(gson.toJson(StoredRecord.of(record)).toByteArray(Charsets.UTF_8))
        }
    }

    fun loadAll(): LoadedCertificates {
        if (!directory.isDirectory()) return LoadedCertificates(emptyList(), emptyList())

        val certificates = mutableListOf<StoredCertificate>()
        val failures = mutableListOf<String>()
        for (metadata in directory.listDirectoryEntries("*.json").sortedBy { it.name }) {
            try {
                certificates += load(metadata)
            } catch (exception: Exception) {
                failures += "${metadata.name}: ${exception.message}"
            }
        }
        return LoadedCertificates(certificates, failures)
    }

    private fun load(metadata: Path): StoredCertificate {
        val record = gson.fromJson(metadata.readText(Charsets.UTF_8), StoredRecord::class.java)?.toRecord()
            ?: error("metadata is empty")

        val imageFile = directory.resolve("${record.mapId}.png")
        require(imageFile.exists()) { "image ${imageFile.name} is missing" }

        val image = ImageIO.read(imageFile.toFile()) ?: error("image ${imageFile.name} is not a readable PNG")
        require(image.width == CertificateArtwork.SIZE && image.height == CertificateArtwork.SIZE) {
            "image ${imageFile.name} has the wrong size ${image.width}x${image.height}"
        }
        return StoredCertificate(record, image)
    }

    private fun writeAtomically(target: Path, write: (OutputStream) -> Unit) {
        val temporary = target.resolveSibling("${target.name}.tmp")
        temporary.outputStream().use(write)
        try {
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, target, REPLACE_EXISTING)
        }
    }

    /** JSON shape of [CertificateRecord]; plain strings keep the file readable and Gson free of type adapters. */
    private data class StoredRecord(
        val mapId: Int? = null,
        val playerUuid: String? = null,
        val playerName: String? = null,
        val eventName: String? = null,
        val date: String? = null,
        val createdBy: String? = null,
        val createdAt: String? = null,
    ) {
        fun toRecord() = CertificateRecord(
            mapId = requireNotNull(mapId) { "mapId is missing" },
            playerUuid = UUID.fromString(requireNotNull(playerUuid) { "playerUuid is missing" }),
            playerName = requireNotNull(playerName) { "playerName is missing" },
            eventName = requireNotNull(eventName) { "eventName is missing" },
            date = LocalDate.parse(requireNotNull(date) { "date is missing" }),
            createdBy = UUID.fromString(requireNotNull(createdBy) { "createdBy is missing" }),
            createdAt = Instant.parse(requireNotNull(createdAt) { "createdAt is missing" }),
        )

        companion object {
            fun of(record: CertificateRecord) = StoredRecord(
                mapId = record.mapId,
                playerUuid = record.playerUuid.toString(),
                playerName = record.playerName,
                eventName = record.eventName,
                date = record.date.toString(),
                createdBy = record.createdBy.toString(),
                createdAt = record.createdAt.toString(),
            )
        }
    }
}
