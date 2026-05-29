package dev.slne.surf.survival.events.race.command


import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.survival.events.race.command.subcommand.checkpointListCommand
import dev.slne.surf.survival.events.race.command.subcommand.getLeaderboardCommand
import dev.slne.surf.survival.events.race.command.subcommand.kickCommand
import dev.slne.surf.survival.events.race.command.subcommand.nextRoundCommand
import dev.slne.surf.survival.events.race.command.subcommand.raceStopCommand
import dev.slne.surf.survival.events.race.command.subcommand.removeCheckpoint
import dev.slne.surf.survival.events.race.command.subcommand.setBarrierCommand
import dev.slne.surf.survival.events.race.command.subcommand.setCheckPointCommand
import dev.slne.surf.survival.events.race.command.subcommand.setLapsCommand
import dev.slne.surf.survival.events.race.command.subcommand.setLobbyCommand
import dev.slne.surf.survival.events.race.command.subcommand.setStartCommand
import dev.slne.surf.survival.events.race.command.subcommand.startRaceCommand
import dev.slne.surf.survival.events.race.command.subcommand.surfModToolsReloadCommand

fun raceCommand() = commandAPICommand("race") {
    withSubcommands(
        startRaceCommand(),
        removeCheckpoint(),
        checkpointListCommand(),
        surfModToolsReloadCommand(),
        raceStopCommand(),
        getLeaderboardCommand(),
        nextRoundCommand(),
        kickCommand(),
        setCheckPointCommand(),
        setStartCommand(),
        setBarrierCommand(),
        setLapsCommand(),
        setLobbyCommand(),
    )
}