package dev.slne.surf.survival.events.race.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.listener.util.setOfflineLocation
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.service.RaceService
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object QuitListener : Listener {
    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (!RaceService.isInRace(player)) return

        RaceService.removePlayer(player)
        plugin.launch {
            val offlinePlayer = Bukkit.getOfflinePlayer(player.uniqueId)
            val location = Location(Bukkit.getWorld("world"), 0.5, 73.0, 0.5, 0f, 0f)
            offlinePlayer.setOfflineLocation(location)
        }

        RaceService.getSpectatorPlayers().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendText {
                appendInfoPrefix()
                info("${player.name} hat das Rennen verlassen.")
            }
        }
    }
}
