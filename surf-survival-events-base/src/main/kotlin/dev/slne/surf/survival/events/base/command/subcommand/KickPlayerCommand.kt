package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry
import org.bukkit.entity.Player

fun CommandTree.kickPlayerCommand() = literalArgument("kick") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)
    entitySelectorArgumentOnePlayer("targetPlayer") {
        playerExecutor { player, args ->
            val targetPlayer: Player by args
            if (!GameService.isInGameQueue(player) && !GameService.isInWaitingQueue(player)) {
                player.sendText {
                    appendErrorPrefix()
                    error("${targetPlayer.name} konnte nicht gekickt werden, da er in keiner Queue ist.")
                }
                return@playerExecutor
            }

            GameService.leaveWaitingQueue(targetPlayer)
            GameService.leaveGameQueue(targetPlayer)

            player.sendText {
                appendSuccessPrefix()
                success("${targetPlayer.name} wurde aus der Queue entfernt.")
            }
        }
    }
}