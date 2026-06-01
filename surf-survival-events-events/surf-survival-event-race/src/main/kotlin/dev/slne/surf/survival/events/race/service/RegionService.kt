package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.game.RaceGame
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.utils.containsComplete
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.util.BoundingBox

object RegionService {
    suspend fun fillBlocks(material: Material) = coroutineScope {
        val context = GameService.snapshot() ?: error("Could not get current game context")

        RaceConfig.getConfig().barriers.forEach { box ->
            val world = context.eventWorld

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

    fun getCheckpoint(playerLocation: Location): RaceConfig.CheckPointConfig? {
        val context = GameService.requiredSnapshot(RaceGame.KEY)
        if (context.eventWorld != playerLocation.world) return null

        return RaceConfig.getConfig()
            .checkpoints
            .find { it.containsComplete(playerLocation) }
    }

    fun getSortedCheckpoints(): List<RaceConfig.CheckPointConfig> =
        RaceConfig.getConfig()
            .checkpoints
            .sortedBy { it.id }

    fun getNextExpectedCheckpointId(currentCheckpointId: Int): Int? {
        val sorted = getSortedCheckpoints()
        if (currentCheckpointId == 0) return sorted.firstOrNull()?.id
        val currentIndex = sorted.indexOfFirst { it.id == currentCheckpointId }
        if (currentIndex == -1) return sorted.firstOrNull()?.id
        return sorted.getOrNull(currentIndex + 1)?.id
    }

    fun getHighestCheckpointId(): Int? =
        RaceConfig.getConfig()
            .checkpoints
            .maxOfOrNull { it.id }

    fun getStart(playerLocation: Location): BoundingBox? {
        val context = GameService.requiredSnapshot(RaceGame.KEY)
        if (context.eventWorld != playerLocation.world) return null

        return RaceConfig.getConfig()
            .starts
            .find { it.containsComplete(playerLocation) }
    }
}
