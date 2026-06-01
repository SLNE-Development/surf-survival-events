package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.survival.events.race.command.util.createRegionCommand
import dev.slne.surf.survival.events.race.config.RaceConfig

fun CommandAPICommand.setStartCommand() = createRegionCommand(
    name = "set-start",
    requiresRotation = true
) { pos1, pos2, rotation ->

    rotation ?: return@createRegionCommand

    RaceConfig.edit {
        starts.clear()

        starts.add(
            RaceConfig.StartConfig(
                x1 = pos1.x, y1 = pos1.y, z1 = pos1.z,
                x2 = pos2.x, y2 = pos2.y, z2 = pos2.z,
                yaw = rotation.yaw,
                pitch = rotation.pitch
            )
        )
    }
}