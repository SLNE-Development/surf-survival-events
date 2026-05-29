package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.region.boundingBox
import dev.slne.surf.survival.events.race.region.contains
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

object RegionService {
    suspend fun fillBlocks(material: Material) = coroutineScope {
        SurfRaceConfig.getConfig().barrier.forEach { barrier ->
            val world = Bukkit.getWorld(barrier.world) ?: return@forEach
            val box = barrier.boundingBox

            val minX = box.minX.toInt()
            val minY = box.minY.toInt()
            val minZ = box.minZ.toInt()

            val maxX = box.maxX.toInt()
            val maxY = box.maxY.toInt()
            val maxZ = box.maxZ.toInt()

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    for (z in minZ..maxZ) {
                        launch(
                            plugin.regionDispatcher(
                                Location(
                                    world,
                                    x.toDouble(),
                                    y.toDouble(),
                                    z.toDouble()
                                )
                            )
                        ) {
                            world.getBlockAt(x, y, z).type = material
                        }
                    }
                }
            }
        }
    }

    fun getCheckpoint(playerLocation: Location): SurfRaceConfig.CheckPointConfig? =
        SurfRaceConfig.getConfig()
            .checkPoints
            .find { it.contains(playerLocation) }

    fun getSortedCheckpoints(): List<SurfRaceConfig.CheckPointConfig> =
        SurfRaceConfig.getConfig()
            .checkPoints
            .sortedBy { it.id }

    fun getNextExpectedCheckpointId(currentCheckpointId: Int): Int? {
        val sorted = getSortedCheckpoints()
        if (currentCheckpointId == 0) return sorted.firstOrNull()?.id
        val currentIndex = sorted.indexOfFirst { it.id == currentCheckpointId }
        if (currentIndex == -1) return sorted.firstOrNull()?.id
        return sorted.getOrNull(currentIndex + 1)?.id
    }

    fun getHighestCheckpointId(): Int? =
        SurfRaceConfig.getConfig()
            .checkPoints
            .maxOfOrNull { it.id }

    fun getStart(playerLocation: Location): SurfRaceConfig.StartConfig? = SurfRaceConfig.getConfig()
        .start
        .find { it.contains(playerLocation) }
}
