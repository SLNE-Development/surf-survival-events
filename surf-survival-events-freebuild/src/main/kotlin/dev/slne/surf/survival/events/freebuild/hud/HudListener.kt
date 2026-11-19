package dev.slne.surf.survival.events.freebuild.hud

import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object HudListener : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (FreebuildPartConfig.getConfig().enableSurvivalEventsNpc) {
            HudManager.show(event.player)
        }
    }
}