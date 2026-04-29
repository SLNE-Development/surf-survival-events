package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.games.service.AnnouncementService
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun stopEventCommand() = subcommand("stop") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    playerExecutor { player, _ ->

        val activeGame = GameService.getActiveGame()

        if (GameService.isGameActive()) {
            player.sendText {
                appendWarningPrefix()
                warning("Das Spiel ")
                variableValue(activeGame.displayName)
                appendSpace()
                warning("wurde deaktiviert!")
            }
            
            for (onlinePlayer in server.onlinePlayers) {
                AnnouncementService.sendCloseEvent(onlinePlayer)
            }
            
            GameService.stopGame()
            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            error("Es ist kein Event Aktiv!")
        }
    }
}