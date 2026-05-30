package dev.slne.surf.survival.events.base.listeners

import dev.slne.surf.survival.events.base.service.GameService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object JoinQuitListener : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        GameService.onPlayerQuit(event.player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        GameService.onPlayerJoin(event.player)
    }
}