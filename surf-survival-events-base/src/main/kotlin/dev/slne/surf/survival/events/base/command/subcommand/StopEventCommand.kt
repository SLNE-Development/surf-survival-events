package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.service.AnnouncementService
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun CommandTree.stopEventCommand() = literalArgument("stop") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist kein Event aktiv!")
            }
            return@playerExecutor
        }

        val activeGame = GameService.getActiveGame()

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
}