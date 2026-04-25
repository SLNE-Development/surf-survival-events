package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun beginGameCommand() = subcommand("begin") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist derzeit kein Event aktiv.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendSuccessPrefix()
            success("Event wird gestartet...")
        }

        //TODO: API that start the Game that you want
        GameService.stopGame()
    }
}