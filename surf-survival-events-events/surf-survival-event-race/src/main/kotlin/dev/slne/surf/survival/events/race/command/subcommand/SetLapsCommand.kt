package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig

fun CommandAPICommand.setLapsCommand() = subcommand("set-laps") {
    integerArgument("value", 1)
    anyExecutor { sender, args ->
        val value: Int by args

        SurfRaceConfig.edit {
            laps = value
        }
        SurfRaceConfig.save()

        sender.sendText {
            appendSuccessPrefix()
            success("Die Anzahl der Runden wurde auf")
            appendSpace()
            variableValue(value.toString())
            appendSpace()
            success("gesetzt.")
        }
    }
}