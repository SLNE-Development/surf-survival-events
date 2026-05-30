package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.RemoveResult
import dev.slne.surf.survival.events.base.util.PermissionRegistry

fun CommandTree.leaveQueueCommand() = literalArgument("leave") {
    withPermission(PermissionRegistry.COMMAND_PLAYER)

    playerExecutor { player, _ ->
        when (GameService.remove(player, includeSpectators = true)) {
            RemoveResult.NO_ACTIVE_GAME -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Derzeit ist kein Event aktiv.")
                }
            }

            RemoveResult.NOT_PARTICIPATING -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist aktuell nicht für das Event eingetragen.")
                }
            }

            RemoveResult.REMOVED_FROM_LOBBY,
            RemoveResult.REMOVED_FROM_WAITING_LIST -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast die Queue verlassen.")
                }
            }

            RemoveResult.REMOVED_SPECTATOR -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Du bist kein Zuschauer des Events mehr.")
                }
            }
        }
    }
}