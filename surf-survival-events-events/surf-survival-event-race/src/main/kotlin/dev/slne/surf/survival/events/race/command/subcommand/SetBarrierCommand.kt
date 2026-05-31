package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.survival.events.race.command.util.createRegionCommand
import dev.slne.surf.survival.events.race.config.RaceConfig
import org.bukkit.util.BoundingBox

fun CommandAPICommand.setBarrierCommand() = createRegionCommand("set-barrier") { pos1, pos2, _ ->

    RaceConfig.edit {
        barriers.clear()
        barriers.add(BoundingBox.of(pos1, pos2))
    }
}