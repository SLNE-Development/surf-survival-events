package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionList

fun CommandAPICommand.setPlayersPerRoundCommand() = subcommand("set-players-per-round") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("value", 1)
    anyExecutor { sender, args ->
        val value: Int by args

        RaceConfig.edit {
            gameplay.playersPerRound = value
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Aktive Racer pro Runde wurden auf")
            appendSpace()
            variableValue(value.toString())
            appendSpace()
            success("gesetzt.")
        }
    }
}
