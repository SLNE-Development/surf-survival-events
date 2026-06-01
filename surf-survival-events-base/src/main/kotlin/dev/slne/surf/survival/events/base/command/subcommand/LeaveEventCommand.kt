package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.RemoveResult
import dev.slne.surf.survival.events.base.util.PermissionRegistry

internal fun CommandTree.leaveEventCommand() = literalArgument("leave") {
    withPermission(PermissionRegistry.COMMAND_PLAYER)

    playerExecutor { player, _ ->
        when (GameService.remove(player, includeSpectators = true)) {
            RemoveResult.NO_ACTIVE_GAME -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Derzeit ist kein Event aktiv.")
                }
            }

            RemoveResult.EVENT_BUSY -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Das Event startet oder stoppt gerade.")
                }
            }

            RemoveResult.NOT_PARTICIPATING -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist aktuell nicht im Event eingetragen.")
                }
            }

            RemoveResult.REMOVED_PLAYER,
            RemoveResult.REMOVED_RESERVE -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast das Event verlassen.")
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
