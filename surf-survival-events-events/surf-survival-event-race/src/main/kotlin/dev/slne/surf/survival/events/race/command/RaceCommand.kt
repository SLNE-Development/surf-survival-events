package dev.slne.surf.survival.events.race.command


import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.survival.events.race.command.subcommand.*

fun raceCommand() = commandAPICommand("race") {
    startRaceCommand()
    removeCheckpoint()
    checkpointListCommand()
    surfModToolsReloadCommand()
    raceStopCommand()
    getLeaderboardCommand()
    nextRoundCommand()
    kickCommand()
    setCheckPointCommand()
    setStartCommand()
    setBarrierCommand()
    setLapsCommand()
    setPlayersPerRoundCommand()
    setQualifiersPerRoundCommand()
    setFinalWinnerCountCommand()
    setLobbyCommand()
    setSpectatorCommand()
}