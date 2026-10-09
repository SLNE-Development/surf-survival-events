package dev.slne.surf.survival.events.werewolf.listeners

import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent

object WerewolfDisconnectListener : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        WerewolfGameManager.handleDisconnect(event.player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        WerewolfGameManager.handleDisconnect(event.player)
    }

}
