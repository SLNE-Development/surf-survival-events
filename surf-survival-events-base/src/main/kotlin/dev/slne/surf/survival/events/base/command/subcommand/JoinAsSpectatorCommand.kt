package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.SpectatorJoinResult
import dev.slne.surf.survival.events.base.util.PermissionRegistry

fun CommandTree.joinAsSpectatorCommand() = literalArgument("spectator") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)

    playerExecutor { player, _ ->
        when (GameService.joinSpectator(player)) {
            SpectatorJoinResult.NO_ACTIVE_GAME -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Derzeit ist kein Event aktiv.")
                }
            }

            SpectatorJoinResult.ALREADY_PLAYER -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist bereits als Spieler dabei!")
                }
            }

            SpectatorJoinResult.ALREADY_SPECTATOR -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist bereits als Zuschauer dabei.")
                }
            }

            SpectatorJoinResult.JOINED -> {
                player.sendText {
                    appendInfoPrefix()
                    info("Du bist jetzt Zuschauer des Events.")
                }
            }
        }
    }
}