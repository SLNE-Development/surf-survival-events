package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.RemoveResult
import dev.slne.surf.survival.events.base.util.PermissionRegistry
import org.bukkit.entity.Player

fun CommandTree.kickPlayerCommand() = literalArgument("kick") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)

    entitySelectorArgumentOnePlayer("targetPlayer") {
        playerExecutor { player, args ->
            val targetPlayer: Player by args

            when (GameService.remove(targetPlayer, includeSpectators = false)) {
                RemoveResult.NO_ACTIVE_GAME -> {
                    player.sendText {
                        appendErrorPrefix()
                        error("Derzeit ist kein Event aktiv.")
                    }
                }

                RemoveResult.NOT_PARTICIPATING,
                RemoveResult.REMOVED_SPECTATOR -> {
                    player.sendText {
                        appendErrorPrefix()
                        error("${targetPlayer.name} konnte nicht gekickt werden, da er nicht als Spieler eingetragen ist.")
                    }
                }

                RemoveResult.REMOVED_FROM_LOBBY,
                RemoveResult.REMOVED_FROM_WAITING_LIST -> {
                    targetPlayer.sendText {
                        appendInfoPrefix()
                        info("Du wurdest aus dem Event entfernt.")
                    }

                    player.sendText {
                        appendSuccessPrefix()
                        success("${targetPlayer.name} wurde aus dem Event entfernt.")
                    }
                }
            }
        }
    }
}