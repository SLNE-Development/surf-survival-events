package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.OpenRoundResult
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList

fun CommandAPICommand.openRoundCommand() = subcommand("openRound") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    stringArgument("id", optional = true)

    playerExecutor { player, arguments ->
        if (!GameService.isActiveGame(RedLightGreenLightGame.KEY)) {
            player.sendText {
                appendErrorPrefix()
                error("Red Light, Green Light ist nicht aktiv. Starte das Event zuerst mit /survivalevents start red_light_green_light.")
            }
            return@playerExecutor
        }

        val id: String? by arguments

        when (val result = RlglService.openRound(player, id)) {
            is OpenRoundResult.Opened -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Runde")
                    appendSpace()
                    variableValue(result.id)
                    appendSpace()
                    success("wurde geöffnet. Spieler in der Nähe wurden eingeladen, mit /rlgl join beizutreten.")
                }
            }

            OpenRoundResult.AlreadyOpen -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist bereits eine Runde offen.")
                }
            }

            OpenRoundResult.RoundInProgress -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es läuft bereits eine Runde.")
                }
            }
        }
    }
}