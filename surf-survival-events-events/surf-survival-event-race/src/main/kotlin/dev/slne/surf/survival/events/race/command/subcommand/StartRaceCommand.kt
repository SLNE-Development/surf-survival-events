package dev.slne.surf.survival.events.race.command.subcommand

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.game.RaceGame
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionList
import org.bukkit.Material

fun CommandAPICommand.startRaceCommand() = subcommand("start") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)

    playerExecutorSuspend { player, _ ->
        if (!GameService.isActiveGame(RaceGame.KEY) && RaceService.getRaceState() == RaceState.DEACTIVATED) {
            player.sendText {
                appendErrorPrefix()
                error("Race ist nicht aktiv. Starte das Event zuerst mit /survivalevents start race.")
            }
            return@playerExecutorSuspend
        }

        if (RaceService.getRaceState() == RaceState.DEACTIVATED) {
            player.sendText {
                appendErrorPrefix()
                error("Race wurde vom Base-Service noch nicht initialisiert.")
            }
            return@playerExecutorSuspend
        }

        if (RaceService.getRaceState() == RaceState.LOBBY) {
            player.sendText {
                appendSuccessPrefix()
                success("Racer werden an den Start gesetzt...")
            }

            plugin.launch {
                RegionService.fillBlocks(Material.BARRIER)
                RaceService.playerToStartMid()
            }

            return@playerExecutorSuspend
        }

        if (RaceService.getRaceState() == RaceState.WAITING) {
            player.sendText {
                appendSuccessPrefix()
                success("Das Rennen startet...")
            }
            RaceService.setRaceState(RaceState.COUNTDOWN)
            RaceService.startCountdown()
            return@playerExecutorSuspend
        }

        player.sendText {
            appendErrorPrefix()
            error("Du kannst den Command gerade nicht ausführen.")
            appendNewline()
            error("Aktueller State:")
            appendSpace()
            variableValue(RaceService.getRaceState().name)
        }
    }
}
