package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry
import org.bukkit.entity.Player

fun kickPlayerCommand() = subcommand("kick") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    entitySelectorArgumentOnePlayer("targetPlayer")
    playerExecutor { player, args ->
        val targetPlayer: Player by args
        if (!GameService.isInGameQueue(player) || !GameService.isInWaitingQueue(player)) {
            player.sendText {
                appendErrorPrefix()
                error("${targetPlayer.name} konnte nicht gekickt werden, da er in keiner Queue ist.")
            }
        }

        GameService.leaveWaitingQueue(player)
        GameService.leaveGameQueue(player)

        player.sendText {
            appendSuccessPrefix()
            success("${targetPlayer.name} wurde aus der Queue entfernt.")
        }
    }
}