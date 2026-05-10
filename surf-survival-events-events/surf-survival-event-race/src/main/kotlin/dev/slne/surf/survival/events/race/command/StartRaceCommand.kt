package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand

fun startRaceCommand() = subcommand("start") {
    withSubcommands(
    startCountDownCommand(),
    startEventCommand(),
    )
    anyExecutor { sender, _ ->  }
}