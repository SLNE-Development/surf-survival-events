package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.AnnouncementService
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.PermissionRegistry

internal fun CommandTree.stopEventCommand() = literalArgument("stop") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    playerExecutorSuspend { player, _ ->
        val stoppedGame = GameService.stopGameAndWait(GameStopReason.COMMAND)

        if (stoppedGame == null) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist kein Event aktiv!")
            }
            return@playerExecutorSuspend
        }

        player.sendText {
            appendWarningPrefix()
            warning("Das Event")
            appendSpace()
            variableValue(stoppedGame.displayName)
            appendSpace()
            warning("wurde gestoppt!")
        }

        AnnouncementService.broadcastCloseEvent(stoppedGame)
    }
}