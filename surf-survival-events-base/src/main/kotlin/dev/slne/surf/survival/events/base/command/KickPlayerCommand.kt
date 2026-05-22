package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry
import org.bukkit.entity.Player

fun kickPlayerCommand() = subcommand("kick") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, args ->
        val targetPlayer: Player by args
        if (!GameService.isInGameQueue(player) || !GameService.isInWaitingQueue(player)) {
            player.sendText {
                appendErrorPrefix()
                error("${targetPlayer.name} konnte nicht gekickt werden, da er sich in keiner Warteschlange befindet.")
            }
        }

        GameService.leaveWaitingQueue(player)
        GameService.leaveGameQueue(player)

        player.sendText {
            appendSuccessPrefix()
            success("${targetPlayer.name} wurde von der Warteschlange entfernt.")
        }
    }
}