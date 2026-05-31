package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.survival.events.race.command.util.createRegionCommand
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.region.clearBoundingBoxCache

fun CommandAPICommand.setBarrierCommand() = createRegionCommand("set-barrier") { pos1, pos2, _ ->

    RaceConfig.edit {
        eventWorld = pos1.world.name

        barriers.clear()

        barriers.add(
            RaceConfig.BarrierConfig(

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

    clearBoundingBoxCache()
    RaceConfig.save()
}