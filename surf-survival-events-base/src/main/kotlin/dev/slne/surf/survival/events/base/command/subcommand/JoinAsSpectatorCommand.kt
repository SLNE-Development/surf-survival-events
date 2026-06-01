package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.JoinEventType
import dev.slne.surf.survival.events.base.util.PermissionRegistry

internal fun CommandTree.joinAsSpectatorCommand() = literalArgument("spectator") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)

    playerExecutorSuspend { player, _ ->
        when (GameService.joinSpectator(player).type) {
            JoinEventType.NO_ACTIVE_GAME -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Derzeit ist kein Event aktiv.")
                }
            }

            JoinEventType.EVENT_BUSY -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Das Event startet oder stoppt gerade.")
                }
            }

            JoinEventType.SPECTATORS_DISABLED -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Dieses Event erlaubt keine Zuschauer.")
                }
            }

            JoinEventType.ALREADY_PARTICIPATING -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist bereits als Spieler dabei!")
                }
            }

            JoinEventType.ALREADY_SPECTATOR -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist bereits als Zuschauer dabei.")
                }
            }

            JoinEventType.JOINED_AS_SPECTATOR -> {
                player.sendText {
                    appendInfoPrefix()
                    info("Du bist jetzt Zuschauer des Events.")
                }
            }

            JoinEventType.RUNNING_JOIN_DENIED,
            JoinEventType.JOINED_AS_PLAYER,
            JoinEventType.JOINED_AS_RESERVE -> Unit
        }
    }
}