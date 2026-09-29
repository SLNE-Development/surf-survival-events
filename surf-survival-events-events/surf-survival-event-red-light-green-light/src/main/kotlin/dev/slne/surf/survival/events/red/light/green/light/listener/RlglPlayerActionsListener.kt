package dev.slne.surf.survival.events.red.light.green.light.listener

import dev.slne.surf.api.core.messages.adventure.uuid
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

object RlglPlayerActionsListener : Listener {

    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val rawDamager = event.damager
        val damager = (rawDamager as? Projectile)?.shooter as? Player ?: rawDamager as? Player ?: return

        if (RlglService.isActiveInRound(damager.uuid()) || RlglService.isActiveInRound(victim.uuid())) {
            event.isCancelled = true
        }
    }
}