package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun leaveGameQueueCommand() = subcommand("leave") {
    withPermission(PermissionRegistry.COMMAND_PLAYER)

    playerExecutor { player, _ ->
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist derzeit kein Event aktiv.")
            }
            return@playerExecutor
        }

        if (GameService.leaveGameQueue(player) || GameService.leaveWaitingQueue(player)) {
            player.sendText {
                appendSuccessPrefix()
                success("Du hast die Warteschlange verlassen.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            error("Du bist nicht in der Warteschlange.")
        }
    }
}