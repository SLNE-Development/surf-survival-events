package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor

fun openMainMenu() = commandAPICommand("event") {
    withSubcommands(
        startEventCommand(),
        joinGameQueueCommand(),
        leaveGameQueueCommand(),
        eventMenuCommand(),
        changeMaxPlayersCommand(),
        stopEventCommand(),
        joinAsSpectatorCommand(),
    )
    
    playerExecutor { _, _ -> }
}
