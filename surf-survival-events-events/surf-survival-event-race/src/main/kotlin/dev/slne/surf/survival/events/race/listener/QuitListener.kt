package dev.slne.surf.survival.events.race.listener

import dev.slne.surf.survival.events.race.service.RaceService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object QuitListener : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        RaceService.removePlayer(player)
    }
}