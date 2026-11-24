package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.JoinRoundResult
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService

fun CommandAPICommand.joinCommand() = subcommand("join") {
    stringArgument("id", optional = true)

    playerExecutor { player, arguments ->
        if (!GameService.isActiveGame(RedLightGreenLightGame.KEY)) {
            player.sendText {
                appendErrorPrefix()
                error("Red Light, Green Light ist nicht aktiv.")
            }
            return@playerExecutor
        }

        val id: String? by arguments

        when (RlglService.joinRound(player, id)) {
            JoinRoundResult.JOINED -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Du bist der Runde beigetreten! Warte, bis der Community-Manager sie startet.")
                }
            }

            JoinRoundResult.ALREADY_JOINED -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du bist der Runde bereits beigetreten.")
                }
            }

            JoinRoundResult.NO_ROUND_OPEN -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist aktuell keine Runde offen.")
                }
            }

            JoinRoundResult.WRONG_ID -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist keine Runde mit dieser ID offen.")
                }
            }
        }
    }
}