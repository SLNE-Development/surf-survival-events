package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.AnnouncementService
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.PermissionRegistry

fun CommandTree.stopEventCommand() = literalArgument("stop") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    playerExecutor { player, _ ->
        val stoppedGame = GameService.stopGame()

        if (stoppedGame == null) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist kein Event aktiv!")
            }
            return@playerExecutor
        }

        player.sendText {
            appendWarningPrefix()
            warning("Das Spiel")
            appendSpace()
            variableValue(stoppedGame.displayName)
            appendSpace()
            warning("wurde deaktiviert!")
        }

        AnnouncementService.broadcastCloseEvent()
    }
}