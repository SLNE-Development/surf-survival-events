package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun leaveQueueCommand() = subcommand("leave") {
    withPermission(PermissionRegistry.COMMAND_PLAYER)
    playerExecutor { player, _ ->
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Derzeit ist kein Event aktiv.")
            }
            return@playerExecutor
        }

        if (GameService.leaveGameQueue(player) || GameService.leaveWaitingQueue(player) || GameService.removeSpectator(player)) {
            player.sendText {
                appendSuccessPrefix()
                success("Du hast die Queue verlassen.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            error("Du stehst aktuell in keiner Queue.")
        }
    }
}