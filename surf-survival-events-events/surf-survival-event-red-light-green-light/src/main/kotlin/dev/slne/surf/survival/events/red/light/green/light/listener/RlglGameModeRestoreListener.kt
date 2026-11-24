package dev.slne.surf.survival.events.red.light.green.light.listener

import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object RlglGameModeRestoreListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        RlglService.applyPendingGameModeRestore(event.player)
    }
}