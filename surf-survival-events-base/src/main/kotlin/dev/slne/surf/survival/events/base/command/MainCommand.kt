package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText

fun openMainMenu() = commandAPICommand("event") {
    withSubcommands(
        startEventCommand(),
        joinGameQueueCommand(),
        leaveGameQueueCommand(),
        beginGameCommand(),
        eventMenuCommand(),
        changeMaxPlayersCommand(),
        stopEventCommand()

    )
    playerExecutor { player, _ ->
        player.sendText {
            appendErrorPrefix()
            error("Du musst was wählen.")
        }
    }
}
