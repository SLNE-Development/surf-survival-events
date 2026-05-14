package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.region.contains
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.region.maxX
import dev.slne.surf.survival.events.race.region.maxY
import dev.slne.surf.survival.events.race.region.maxZ
import dev.slne.surf.survival.events.race.region.minX
import dev.slne.surf.survival.events.race.region.minY
import dev.slne.surf.survival.events.race.region.minZ
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

object RegionService {

    fun fillBlocks(material: Material) {
        plugin.launch {
            SurfRaceConfig.getConfig().barrier.forEach { barrier ->

                val world = Bukkit.getWorld(barrier.world) ?: return@forEach

                for (x in barrier.minX.toInt()..barrier.maxX.toInt()) {
                    for (y in barrier.minY.toInt()..barrier.maxY.toInt()) {
                        for (z in barrier.minZ.toInt()..barrier.maxZ.toInt()) {

                            val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

                            launch(plugin.regionDispatcher(location)) {
                                world.getBlockAt(x, y, z).type = material
                            }
                        }
                    }
                }
            }
        }
    }

    fun getCheckpoint(playerLocation: Location): SurfRaceConfig.Checkpoint? {
        return SurfRaceConfig.getConfig()
            .checkPoints
            .find { it.contains(playerLocation) }
    }

    fun getStart(playerLocation: Location): SurfRaceConfig.Start? {
        return SurfRaceConfig.getConfig()
            .start
            .find { it.contains(playerLocation) }
    }


}
