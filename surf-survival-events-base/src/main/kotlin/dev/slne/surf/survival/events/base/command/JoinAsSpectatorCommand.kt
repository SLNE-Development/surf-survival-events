package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun joinAsSpectatorCommand() = subcommand("spectator") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)
    playerExecutor { player, _ ->

        val uuid = player.uniqueId
        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist derzeit kein Event aktiv.")
            }
            return@playerExecutor
        }

        if (GameService.isSpectator(uuid)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits drin!")
            }
            return@playerExecutor
        }

        GameService.addSpectator(uuid)
        player.sendText {
            appendInfoPrefix()
            info("Du bist nun Zuschauer des Events.")
        }

    }
}