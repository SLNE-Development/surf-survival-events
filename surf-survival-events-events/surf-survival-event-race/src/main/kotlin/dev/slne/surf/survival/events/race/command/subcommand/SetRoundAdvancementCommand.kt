package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionList

fun CommandAPICommand.setQualifiersPerRoundCommand() = subcommand("set-qualifiers-per-round") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("value", 1)

    anyExecutor { sender, args ->
        val value: Int by args

        RaceConfig.edit {
            gameplay.qualifiersPerRound = value
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Qualifizierte Spieler pro Heat wurden auf")
            appendSpace()
            variableValue(value.toString())
            appendSpace()
            success("gesetzt.")
        }
    }
}

fun CommandAPICommand.setFinalWinnerCountCommand() = subcommand("set-final-winner-count") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("value", 1)

    anyExecutor { sender, args ->
        val value: Int by args

        RaceConfig.edit {
            gameplay.finalWinnerCount = value
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Finale Gewinnerplätze wurden auf")
            appendSpace()
            variableValue(value.toString())
            appendSpace()
            success("gesetzt.")
        }
    }
}
