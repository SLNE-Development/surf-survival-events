package dev.slne.surf.survival.events.race.command.subcommand

import dev.slne.surf.survival.events.race.command.util.createRegionCommand
import dev.slne.surf.survival.events.race.config.SurfRaceConfig


fun setCheckPointCommand()= createRegionCommand("set-checkpoint") { pos1, pos2, _ ->

    val nextNumber =
        (SurfRaceConfig.getConfig().checkPoints.maxOfOrNull { it.id } ?: 0) + 1

    SurfRaceConfig.edit {

        checkPoints.add(
            SurfRaceConfig.CheckPointConfig(

                id = nextNumber,

                world = pos1.world.name,

                x1 = pos1.x,
                y1 = pos1.y,
                z1 = pos1.z,

                x2 = pos2.x,
                y2 = pos2.y,
                z2 = pos2.z
            )
        )
    }
    SurfRaceConfig.save()
}