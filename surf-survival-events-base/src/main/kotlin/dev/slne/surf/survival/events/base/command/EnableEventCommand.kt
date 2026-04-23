package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.command.arguments.gameArgument
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.games.util.Games
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun enableEventCommand() = subcommand("enable") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    gameArgument("game")

    playerExecutor { player, args ->
        val game: Games by args

        if (!Games.isGameEnabled(game.name)) {
            player.sendText {
                appendErrorPrefix()
                variableValue(game.displayName)
                appendSpace()
                error("ist derzeit nicht verfügbar.")
            }
            return@playerExecutor
        }

        if (GameService.enableGame(game)) {
            player.sendText {
                appendSuccessPrefix()
                variableValue(game.displayName)
                appendSpace()
                success("wurde erfolgreich aktiviert.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            variableValue(game.displayName)
            appendSpace()
            error("konnte nicht aktiviert werden. Es ist bereits ein anderes Event aktiv.")
        }

    }
}