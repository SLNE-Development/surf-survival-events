package dev.slne.surf.survival.events.race.listener

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleExitEvent

object RaceListener : Listener {
    @EventHandler
    fun onVehicleExit(event: VehicleExitEvent) {
        val player = event.exited
        if (player !is Player) return

        if (!RaceService.isInRace(player)) return

        if (event.vehicle.type != EntityType.NAUTILUS) return

        player.sendText {
            appendErrorPrefix()
            error("Du darfst dich nicht vom Sattel schmeißen lassen!")
        }
        event.cancel()
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!RaceService.isInRace(player)) return
        if (RaceService.getRaceState() == RaceState.COUNTDOWN || RaceService.getRaceState() == RaceState.WAITING) {
            event.cancel()
        }
    }
}