package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList

fun CommandAPICommand.stopCommand() = subcommand("stop") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)

    playerExecutor { player, _ ->
        if (!GameService.isActiveGame(RedLightGreenLightGame.KEY)) {
            player.sendText {
                appendErrorPrefix()
                error("Red Light, Green Light ist nicht aktiv. Starte das Event zuerst mit /survivalevents start red_light_green_light.")
            }
            return@playerExecutor
        }

        if (!RlglService.stopRound()) {
            player.sendText {
                appendErrorPrefix()
                error("Es läuft aktuell keine Runde.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendSuccessPrefix()
            success("Die Runde wurde gestoppt.")
        }
    }
}