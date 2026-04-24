package dev.slne.surf.survival.events.base.listeners

import dev.slne.surf.survival.events.base.games.service.AnnouncementService
import dev.slne.surf.survival.events.base.games.service.GameService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object JoinQuitListener: Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!GameService.isGameActive()) {
            return
        }
        GameService.leaveGameQueue(event.player)
        GameService.leaveWaitingQueue(event.player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        AnnouncementService.sendAnnouncement(event.player)
    }
}