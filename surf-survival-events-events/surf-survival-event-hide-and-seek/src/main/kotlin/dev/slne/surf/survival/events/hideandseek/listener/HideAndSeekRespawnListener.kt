package dev.slne.surf.survival.events.hideandseek.listener

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekGame
import dev.slne.surf.survival.events.hideandseek.game.SpectatorRole
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

object HideAndSeekRespawnListener : Listener {

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val context = GameService.snapshot()?.takeIf { it.key == HideAndSeekGame.KEY } ?: return
        val config = HideAndSeekConfig.getConfig()

        event.respawnLocation = when {
            HideAndSeekRoleManager.roleOf(event.player) == SpectatorRole ->
                config.spectatorSpawn.toLocation(context)

            HideAndSeekService.isRolePhase -> config.gameSpawn.toLocation(context)
            else -> config.lobbySpawn.toLocation(context)
        }
    }

    @EventHandler
    fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
        plugin.launch {
            HideAndSeekRoleManager.handlePostRespawn(event.player)
        }
    }
}
