package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.game.PlayerRemoveReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.RemoveResult
import dev.slne.surf.survival.events.base.util.PermissionRegistry
import org.bukkit.entity.Player

internal fun CommandTree.kickPlayerCommand() = literalArgument("kick") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)

    entitySelectorArgumentOnePlayer("targetPlayer") {
        playerExecutor { player, args ->
            val targetPlayer: Player by args

            when (GameService.remove(targetPlayer.uniqueId, includeSpectators = true, reason = PlayerRemoveReason.KICK)) {
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
                        error("${targetPlayer.name} ist nicht im Event eingetragen.")
                    }
                }

                RemoveResult.REMOVED_PLAYER,
                RemoveResult.REMOVED_RESERVE,
                RemoveResult.REMOVED_SPECTATOR -> {
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