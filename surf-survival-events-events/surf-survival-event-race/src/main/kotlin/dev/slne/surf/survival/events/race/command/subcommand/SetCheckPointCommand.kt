package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.survival.events.race.command.util.createRegionCommand
import dev.slne.surf.survival.events.race.config.RaceConfig


fun CommandAPICommand.setCheckPointCommand() = createRegionCommand("set-checkpoint") { pos1, pos2, _ ->
    val nextNumber = (RaceConfig.getConfig().checkpoints.maxOfOrNull { it.id } ?: 0) + 1

    RaceConfig.edit {
        checkpoints.add(
            RaceConfig.CheckPointConfig(
                id = nextNumber,
                x1 = pos1.x, y1 = pos1.y, z1 = pos1.z,
                x2 = pos2.x, y2 = pos2.y, z2 = pos2.z
            )
        )
    }
}