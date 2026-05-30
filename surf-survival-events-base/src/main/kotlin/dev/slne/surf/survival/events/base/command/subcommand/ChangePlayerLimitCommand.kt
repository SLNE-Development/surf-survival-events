package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.PermissionRegistry

fun CommandTree.changePlayerLimitCommand() = literalArgument("spielerlimit") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    integerArgument("playerLimit", min = 1) {
        playerExecutor { player, args ->
            val playerLimit: Int by args

            if (!GameService.setMaxPlayers(playerLimit)) {
                player.sendText {
                    appendErrorPrefix()
                    error("Derzeit ist kein Event aktiv.")
                }
                return@playerExecutor
            }

            player.sendText {
                appendSuccessPrefix()
                success("Spielerlimit auf")
                appendSpace()
                variableValue(playerLimit.toString())
                appendSpace()
                success("gesetzt.")
            }
        }
    }
}