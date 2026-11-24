package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.service.StartRoundResult
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList

fun CommandAPICommand.startCommand() = subcommand("start") {
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

        val result = GameService.withGameContext(RedLightGreenLightGame.KEY) {
            RlglService.startRound(id)
        }

        when (result) {
            StartRoundResult.STARTED -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Die Runde startet...")
                }
            }

            StartRoundResult.NO_ROUND_OPEN -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist keine Runde offen. Öffne zuerst eine mit /rlgl openRound.")
                }
            }

            StartRoundResult.WRONG_ID -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist keine Runde mit dieser ID offen.")
                }
            }

            StartRoundResult.NO_PLAYERS_JOINED -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Der Runde ist noch niemand beigetreten.")
                }
            }

            StartRoundResult.ALREADY_RUNNING -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es läuft bereits eine Runde.")
                }
            }
        }
    }
}