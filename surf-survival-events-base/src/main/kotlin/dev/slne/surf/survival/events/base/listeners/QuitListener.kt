package dev.slne.surf.survival.events.base.listeners

import dev.slne.surf.survival.events.base.games.service.GameService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object QuitListener: Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!GameService.isGameActive()) {
            return
        }
        GameService.removePlayerGameQueue(event.player)
    }
}