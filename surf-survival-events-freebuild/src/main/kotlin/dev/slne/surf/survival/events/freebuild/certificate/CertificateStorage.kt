package dev.slne.surf.survival.events.freebuild.certificate

import com.google.gson.GsonBuilder
import java.awt.image.BufferedImage
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.imageio.ImageIO
import kotlin.io.path.*

/**
 * A certificate issued to [playerUuid]. It is pending until the player claims it, then [claimedAt] is set.
 */
data class CertificateRecord(
    val mapId: Int,
    val playerUuid: UUID,
    val playerName: String,
    val eventName: String,
    val date: LocalDate,
    val createdBy: UUID,
    val createdAt: Instant,
    val claimedAt: Instant? = null,
) {
    val isClaimed get() = claimedAt != null
}

/** A metadata [file] that could not be loaded; [mapId] comes from the file name, if it is one. */
data class LoadFailure(val file: String, val mapId: Int?, val reason: String)

data class LoadedRecords(val records: List<CertificateRecord>, val failures: List<LoadFailure>)

/**
 * Stores every certificate as `certificates/<mapId>.png` plus `certificates/<mapId>.json` below the plugin folder.
 * The metadata file is written last, so a certificate only counts as stored once both files exist. Every file is
 * written to a temporary file, flushed to the disk and then moved into place, so a crash never leaves a half-written
 * file behind.
 *
 * Metadata and images are loaded separately, so only the images of maps that are actually shown are read.
 */
class CertificateStorage(dataDirectory: Path) {
    private val directory = dataDirectory.resolve("certificates")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun save(record: CertificateRecord, image: BufferedImage) {
        directory.createDirectories()
        writeAtomically(imageFile(record.mapId)) { output ->
            check(ImageIO.write(image, "png", output)) { "No PNG writer available" }
        }
        saveRecord(record)
    }

    /** Rewrites only the metadata of a certificate whose image is already stored. */
    fun saveRecord(record: CertificateRecord) {
        writeAtomically(directory.resolve("${record.mapId}.json")) { output ->
            output.write(gson.toJson(StoredRecord.of(record)).toByteArray(Charsets.UTF_8))
        }
    }

    /** Loads the metadata of all certificates whose image exists; unreadable certificates are reported as failures. */
    fun loadRecords(): LoadedRecords {
        if (!directory.isDirectory()) return LoadedRecords(emptyList(), emptyList())

        val records = mutableListOf<CertificateRecord>()
        val failures = mutableListOf<LoadFailure>()
        for (metadata in directory.listDirectoryEntries("*.json").sortedBy { it.name }) {
            val mapId = metadata.nameWithoutExtension.toIntOrNull()
            try {
                records += loadRecord(metadata, mapId)
            } catch (exception: Exception) {
                failures += LoadFailure(metadata.name, mapId, exception.message ?: exception.javaClass.name)
            }
        }
        return LoadedRecords(records, failures)
    }

    fun loadImage(mapId: Int): BufferedImage {
        val imageFile = imageFile(mapId)
        val image = ImageIO.read(imageFile.toFile()) ?: error("image ${imageFile.name} is not a readable PNG")
        require(image.width == CertificateArtwork.SIZE && image.height == CertificateArtwork.SIZE) {
            "image ${imageFile.name} has the wrong size ${image.width}x${image.height}"
        }
        return image
    }

    private fun loadRecord(metadata: Path, mapId: Int?): CertificateRecord {
        val record = gson.fromJson(metadata.readText(Charsets.UTF_8), StoredRecord::class.java)?.toRecord()
            ?: error("metadata is empty")
        require(record.mapId == mapId) { "metadata belongs to map #${record.mapId}" }

        val imageFile = imageFile(record.mapId)
        require(imageFile.exists()) { "image ${imageFile.name} is missing" }
        return record
    }

    private fun imageFile(mapId: Int) = directory.resolve("$mapId.png")

    private fun writeAtomically(target: Path, write: (OutputStream) -> Unit) {
        val temporary = target.resolveSibling("${target.name}.tmp")
        FileChannel.open(temporary, WRITE, CREATE, TRUNCATE_EXISTING).use { channel ->
            val output = Channels.newOutputStream(channel).buffered()
            write(output)
            output.flush()
            channel.force(true)
        }
        try {
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary, target, REPLACE_EXISTING)
        }
    }

    /**
     * JSON shape of [CertificateRecord]; plain strings keep the file readable and Gson free of type adapters.
     *
     * Certificates stored before claiming existed were handed out directly and have no [pending] flag, so they
     * count as claimed at their creation time.
     */
    private data class StoredRecord(
        val mapId: Int? = null,
        val playerUuid: String? = null,
        val playerName: String? = null,
        val eventName: String? = null,
        val date: String? = null,
        val createdBy: String? = null,
        val createdAt: String? = null,
        val pending: Boolean? = null,
        val claimedAt: String? = null,
    ) {
        fun toRecord(): CertificateRecord {
            val createdAt = Instant.parse(requireNotNull(createdAt) { "createdAt is missing" })

            return CertificateRecord(
                mapId = requireNotNull(mapId) { "mapId is missing" },
                playerUuid = UUID.fromString(requireNotNull(playerUuid) { "playerUuid is missing" }),
                playerName = requireNotNull(playerName) { "playerName is missing" },
                eventName = requireNotNull(eventName) { "eventName is missing" },
                date = LocalDate.parse(requireNotNull(date) { "date is missing" }),
                createdBy = UUID.fromString(requireNotNull(createdBy) { "createdBy is missing" }),
                createdAt = createdAt,
                claimedAt = if (pending == true) null else claimedAt?.let(Instant::parse) ?: createdAt,
            )
        }

        companion object {
            fun of(record: CertificateRecord) = StoredRecord(
                mapId = record.mapId,
                playerUuid = record.playerUuid.toString(),
                playerName = record.playerName,
                eventName = record.eventName,
                date = record.date.toString(),
                createdBy = record.createdBy.toString(),
                createdAt = record.createdAt.toString(),
                pending = !record.isClaimed,
                claimedAt = record.claimedAt?.toString(),
            )
        }
    }
}
