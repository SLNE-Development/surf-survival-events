package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun changeMaxPlayersCommand() = subcommand("maxPlayers") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    literalArgument("set")
    integerArgument("maxPlayers")
    playerExecutor { player, args ->
        val maxPlayers: Int by args


        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist derzeit kein Event aktiv.")
            }
            return@playerExecutor
        }

        if (maxPlayers <= 0) {
            player.sendText {
                appendErrorPrefix()
                error("Die Spieleranzahl muss größer als 0 sein.")
            }
            return@playerExecutor
        }

        GameService.setMaxPlayers(maxPlayers)

        player.sendText {
            appendSuccessPrefix()
            success("Die maximale Spieleranzahl wurde auf")
            appendSpace()
            variableValue(maxPlayers.toString())
            appendSpace()
            success("gesetzt.")
        }


    }
}