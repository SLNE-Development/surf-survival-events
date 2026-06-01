package dev.slne.surf.survival.events.race.command.subcommand

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.game.RaceGame
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.utils.PermissionList

fun CommandAPICommand.raceStopCommand() = subcommand("stop") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->
        plugin.launch {
            if (!GameService.isActiveGame(RaceGame.KEY) && RaceService.getRaceState() == RaceState.DEACTIVATED) {
                sender.sendText {
                    appendErrorPrefix()
                    error("Es ist kein Race Event aktiv.")
                }
                return@launch
            }

            val stopped = if (GameService.isActiveGame(RaceGame.KEY)) {
                GameService.stopGameAndWait(GameStopReason.COMMAND)
            } else {
                RaceService.stopRace(context = null)
                RaceGame.KEY
            }

            if (stopped == null) {
                sender.sendText {
                    appendErrorPrefix()
                    error("Es ist kein Race Event aktiv.")
                }
                return@launch
            }

            sender.sendText {
                appendSuccessPrefix()
                success("Das Rennen wurde gestoppt.")
            }
        }
    }
}
