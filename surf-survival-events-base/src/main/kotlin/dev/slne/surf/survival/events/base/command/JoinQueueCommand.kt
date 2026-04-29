package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry
import net.kyori.adventure.text.format.TextColor


fun joinGameQueueCommand() = subcommand("join") {
    withPermission(PermissionRegistry.COMMAND_PLAYER)
    playerExecutor { player, _ ->
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist derzeit kein Event aktiv.")
            }
            return@playerExecutor
        }

        if (GameService.isInGameQueue(player) || GameService.isInWaitingQueue(player)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits in der Warteschlange.")
            }
            return@playerExecutor
        }

        if (GameService.isQueueFull()) {
            if (GameService.joinWaitingQueue(player)) {
                player.sendText {
                    appendInfoPrefix()
                    text("Die Warteschlange ist voll. Du befindest dich nun auf der Ersatzbank.", TextColor.color(0x88F288))
                }
                return@playerExecutor
            }
        } else {
            if (GameService.joinGameQueue(player)) {
                player.sendText {
                    appendInfoPrefix()
                    text("Du befindest dich nun in der Warteschlange.", TextColor.color(0x88F288))
                }
                return@playerExecutor
            }
        }
    }
}