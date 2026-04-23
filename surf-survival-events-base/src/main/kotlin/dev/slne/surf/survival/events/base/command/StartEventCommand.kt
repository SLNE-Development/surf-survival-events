package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.command.arguments.gameArgument
import dev.slne.surf.survival.events.base.games.util.Games

fun startEventCommand() = subcommand("enable") {
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
        }
    }
}