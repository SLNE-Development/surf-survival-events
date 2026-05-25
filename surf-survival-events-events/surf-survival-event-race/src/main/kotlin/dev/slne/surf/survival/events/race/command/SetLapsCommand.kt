package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun setLapsCommand() = subcommand("set-laps") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    integerArgument("laps", 1)
    anyExecutor { sender, args ->
        val rawLaps: Int by args

        SurfRaceConfig.edit {
            laps = rawLaps
        }
        SurfRaceConfig.save()

        sender.sendText {
            appendSuccessPrefix()
            success("Die Anzahl der Runden wurde auf")
            appendSpace()
            variableValue(rawLaps.toString())
            appendSpace()
            success("gesetzt.")
        }
    }
}