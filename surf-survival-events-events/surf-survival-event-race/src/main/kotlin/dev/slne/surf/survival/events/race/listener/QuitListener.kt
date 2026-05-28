package dev.slne.surf.survival.events.race.listener

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object QuitListener : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        RaceService.removePlayer(player)

        RaceService.getSpectatorPlayers().forEach {uuid ->
            val playerSpectator = Bukkit.getPlayer(uuid)
            playerSpectator?.sendText {
                appendInfoPrefix()
                info("${player.name} hat das Rennen verlassen.")
            }
        }
    }
}