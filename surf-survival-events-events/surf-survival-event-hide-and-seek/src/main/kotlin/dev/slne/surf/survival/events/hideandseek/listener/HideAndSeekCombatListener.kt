package dev.slne.surf.survival.events.hideandseek.listener

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.game.*
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.service.currentContext
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import kotlinx.coroutines.delay
import org.bukkit.block.data.type.DecoratedPot
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object HideAndSeekCombatListener : Listener {

    private val pendingKills = ConcurrentHashMap.newKeySet<UUID>()

    private fun executeKill(target: Player, killer: Player) {
        plugin.launch(plugin.entityDispatcher(target)) {
            delay(1.ticks)
            pendingKills.add(target.uniqueId)
            try {
                target.damage(Float.MAX_VALUE.toDouble(), killer)
            } finally {
                pendingKills.remove(target.uniqueId)
            }
        }
    }

    @EventHandler
    fun onPrePlayerAttackEntity(event: PrePlayerAttackEntityEvent) {
        val context = currentContext() ?: return
        if (event.player.uniqueId !in context.allEventPlayers) return

        if (!HideAndSeekService.isRolePhase) return event.cancel()

        val role = HideAndSeekRoleManager.roleOf(event.player)
        if (role != SeekerRole && role != HiderRole) {
            event.cancel()
        }
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return

        if (!HideAndSeekService.isSeekingPhase) {
            event.cancel()
        }
    }

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val target = event.entity as? Player ?: return

        if (target.uniqueId in pendingKills) return

        val damager = event.damager as? Player ?: return

        val context = currentContext() ?: return
        if (target.uniqueId !in context.allEventPlayers || damager.uniqueId !in context.allEventPlayers) return

        if (!HideAndSeekService.isSeekingPhase) return event.cancel()

        val damagerRole = HideAndSeekRoleManager.roleOf(damager)
        val targetRole = HideAndSeekRoleManager.roleOf(target)

        if (damagerRole == null || targetRole == null || !damagerRole.canDamage(targetRole)) {
            return event.cancel()
        }

        if (damagerRole == SeekerRole && !HideAndSeekItems.isCatchWeapon(damager.inventory.itemInMainHand)) {
            return event.cancel()
        }

        if (damagerRole == SeekerRole && HideAndSeekConfig.getConfig().gameplay.oneHitKnockOut) {
            executeKill(target, damager)
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val shooter = event.entity.shooter as? Player ?: return
        val context = currentContext() ?: return
        if (shooter.uniqueId !in context.allEventPlayers) return

        if (event.hitBlock?.blockData is DecoratedPot) {
            return event.cancel()
        }

        val target = event.hitEntity as? Player ?: return
        if (target.uniqueId !in context.allEventPlayers) return

        if (!HideAndSeekService.isSeekingPhase) return event.cancel()

        val shooterRole = HideAndSeekRoleManager.roleOf(shooter)
        val targetRole = HideAndSeekRoleManager.roleOf(target)

        if (shooterRole == null || targetRole == null || !shooterRole.canDamage(targetRole)) {
            return event.cancel()
        }

        if (shooterRole == SeekerRole && HideAndSeekConfig.getConfig().gameplay.oneHitKnockOut) {
            executeKill(target, shooter)
        }
    }

    @EventHandler
    fun onEntityShootBow(event: EntityShootBowEvent) {
        val shooter = event.entity as? Player ?: return
        val context = currentContext() ?: return
        if (shooter.uniqueId !in context.allEventPlayers) return
        if (HideAndSeekRoleManager.roleOf(shooter) != SeekerRole) return

        val arrow = event.projectile as? Arrow ?: return
        arrow.lifetimeTicks = 0
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val role = HideAndSeekRoleManager.roleOf(player) ?: return

        event.drops.clear()
        event.deathMessage(null)

        plugin.launch {
            if (role == HiderRole) {
                HideAndSeekRoleManager.announceDeath(player, role)
                HideAndSeekRoleManager.onHiderCaught(player)
            } else {
                HideAndSeekService.performPlayerCheck()
            }
        }
    }
}
