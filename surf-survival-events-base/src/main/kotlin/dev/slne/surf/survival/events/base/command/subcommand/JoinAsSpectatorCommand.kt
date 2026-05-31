package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun CommandTree.joinAsSpectatorCommand() = literalArgument("spectator") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)
    playerExecutor { player, _ ->
        val uuid = player.uniqueId
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Derzeit ist kein Event aktiv.")
            }
            return@playerExecutor
        }

        if (GameService.isInGameQueue(player) || GameService.isInWaitingQueue(player)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits als Spieler dabei!")
            }
            return@playerExecutor
        }

        if (GameService.isSpectator(uuid)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits dabei.")
            }
            return@playerExecutor
        }

        GameService.addSpectator(uuid)
        player.sendText {
            appendInfoPrefix()
            info("Du bist jetzt Zuschauer des Events.")
        }

    }
}