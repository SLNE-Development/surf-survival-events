package dev.slne.surf.survival.events.race.command


import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.survival.events.race.command.subcommand.checkpointListCommand
import dev.slne.surf.survival.events.race.command.subcommand.getLeaderboardCommand
import dev.slne.surf.survival.events.race.command.subcommand.kickCommand
import dev.slne.surf.survival.events.race.command.subcommand.nextRoundCommand
import dev.slne.surf.survival.events.race.command.subcommand.raceStopCommand
import dev.slne.surf.survival.events.race.command.subcommand.removeCheckpoint
import dev.slne.surf.survival.events.race.command.subcommand.setCommands
import dev.slne.surf.survival.events.race.command.subcommand.startRaceCommand
import dev.slne.surf.survival.events.race.command.subcommand.surfModToolsReloadCommand

fun raceCommand() = commandTree("race") {

    setCommands()
    startRaceCommand()
    removeCheckpoint()
    checkpointListCommand()
    surfModToolsReloadCommand()
    raceStopCommand()
    getLeaderboardCommand()
    nextRoundCommand()
    kickCommand()
}