package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandAPICommand

fun raceCommand() = commandAPICommand("race") {
    withSubcommands(
        startRaceCommand(),
        setLocationConfigCommand(),
        setStartCommand(),
        setCheckpointCommand(),
        setBarrierCommand(),
        removeCheckpoint(),
        checkpointListCommand(),
        surfModToolsReloadCommand(),
        raceStopCommand(),
    )
    anyExecutor { _, _ -> }
}