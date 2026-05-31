package dev.slne.surf.survival.events.race.listener.util

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.nbt.*
import net.kyori.adventure.nbt.BinaryTagIO.Compression.GZIP
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import java.io.File
import java.io.IOException
import java.util.*

@OptIn(NmsUseWithCaution::class)
private fun getPlayerFile(uuid: UUID): File? {
    val playerFile = File(SurfPaperNmsPlayerBridge.getPlayerDataDir(), "$uuid.dat")
    if (playerFile.exists()) {
        return playerFile
    }
    return null
}

suspend fun OfflinePlayer.setOfflineLocation(location: Location) {
    this.uniqueId.setOfflineLocation(location)
}

suspend fun UUID.setOfflineLocation(location: Location) = withContext(Dispatchers.IO) {
    val dataFile: File = getPlayerFile(this@setOfflineLocation) ?: return@withContext
    val rawTag: CompoundBinaryTag
    try {
        rawTag = BinaryTagIO.unlimitedReader()
            .read(dataFile.toPath(), GZIP)
    } catch (e: IOException) {
        error("Failed to read player data file for UUID $this: ${e.message}")
    }

    val builder = CompoundBinaryTag.builder().put(rawTag)
    val posTag = ListBinaryTag.builder()
    val rotTag = ListBinaryTag.builder()

    posTag.add(DoubleBinaryTag.doubleBinaryTag(location.x))
    posTag.add(DoubleBinaryTag.doubleBinaryTag(location.y))
    posTag.add(DoubleBinaryTag.doubleBinaryTag(location.z))

    rotTag.add(FloatBinaryTag.floatBinaryTag(location.yaw))
    rotTag.add(FloatBinaryTag.floatBinaryTag(location.pitch))

    builder.put("Pos", posTag.build())
    builder.put("Rotation", rotTag.build())

    val worldUUIDLeast: Long = location.getWorld().uid.leastSignificantBits
    val worldUUIDMost: Long = location.getWorld().uid.mostSignificantBits

    builder.putLong("WorldUUIDLeast", worldUUIDLeast)
    builder.putLong("WorldUUIDMost", worldUUIDMost)

    BinaryTagIO.writer()
        .write(builder.build(), dataFile.toPath(), GZIP)
}