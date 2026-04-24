package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText


fun openMainMenu() = commandAPICommand("event") {
    withSubcommands(
        beginEventCommand(),
        joinGameQueueCommand(),
        leaveGameQueueCommand(),
        startGameCommand(),
        eventMenuCommand(),
        changeMaxPlayersCommand()

    )
    playerExecutor { player, _ ->
        player.sendText {
            appendSuccessPrefix()
            error("Du musst was wählen.")
        }
    }
}
