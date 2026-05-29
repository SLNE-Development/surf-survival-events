package dev.slne.surf.survival.events.race.command.subcommand

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.service.ProgressService
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Bukkit
import org.bukkit.Material

fun raceStopCommand() = subcommand("stop") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->

        if (RaceService.getRaceState() == RaceState.DEACTIVATED) {
            sender.sendText {
                appendErrorPrefix()
                error("Es ist kein Event Aktiv.")
            }
            return@anyExecutor
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Das Rennen wurde gestoppt.")
        }

        RaceService.setRaceState(RaceState.DEACTIVATED)
        ProgressService.clear()

        plugin.launch {
            RegionService.fillBlocks(Material.AIR)
        }

        RaceService.getRacePlayers().toList().forEach { uuid ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach
            RaceService.removePlayer(player)
            player.sendText {
                appendInfoPrefix()
                info("Das Rennen wurde gestoppt.")
            }
        }

        RaceService.getSpectatorPlayers().toList().forEach { uuid ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach
            RaceService.removeSpectator(uuid)
            player.sendText {
                appendInfoPrefix()
                info("Das Rennen wurde gestoppt.")
            }
        }
    }
}