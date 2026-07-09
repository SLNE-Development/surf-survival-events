package dev.slne.surf.survival.events.hideandseek.listener

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekItems
import dev.slne.surf.survival.events.hideandseek.game.HiderRole
import dev.slne.surf.survival.events.hideandseek.game.SeekerRole
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.service.currentContext
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.data.type.DecoratedPot
import org.bukkit.block.data.type.Door
import org.bukkit.block.data.type.TrapDoor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent

object HideAndSeekRestrictionListener : Listener {

    private fun isBypassing(player: Player): Boolean = HideAndSeekRoleManager.isBypassing(player)

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return
        if (event.foodLevel < 20) {
            event.cancel()
            player.foodLevel = 20
        }
    }

    @EventHandler
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        val player = event.entity as? Player ?: return
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return
        when (event.regainReason) {
            RegainReason.MAGIC, RegainReason.MAGIC_REGEN -> {}
            else -> event.cancel()
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return

        val item = event.item
        val specialItemId = HideAndSeekItems.specialItemId(item)
        if (item != null && specialItemId != null) {
            event.cancel()
            if (HideAndSeekRoleManager.roleOf(player) == SeekerRole) {
                plugin.launch {
                    HideAndSeekItems.handleSpecialItem(player, item, specialItemId)
                }
            }
            return
        }

        val block = event.clickedBlock ?: return
        if (block.blockData is Door || block.blockData is TrapDoor) return
        if (Tag.BUTTONS.isTagged(block.type)) return
        if (isBypassing(player)) return

        if (player.inventory.itemInMainHand.type == Material.BOW) {
            if (block.blockData is DecoratedPot) {
                event.cancel()
            }
            return
        }

        event.cancel()
    }

    @EventHandler
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val context = currentContext() ?: return
        val remover = event.remover as? Player ?: return event.cancel()
        if (remover.uniqueId !in context.allEventPlayers) return
        if (!isBypassing(remover)) {
            event.cancel()
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val context = currentContext() ?: return
        if (event.player.uniqueId !in context.allEventPlayers) return
        if (!isBypassing(event.player)) {
            event.cancel()
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return
        if (!isBypassing(player)) {
            event.cancel()
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return
        if (!isBypassing(player)) {
            event.cancel()
        }
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return

        val item = event.offHandItem
        val specialItemId = HideAndSeekItems.specialItemId(item)
        if (specialItemId != null) {
            event.cancel()
            if (HideAndSeekRoleManager.roleOf(player) == SeekerRole) {
                plugin.launch {
                    HideAndSeekItems.handleSpecialItem(player, item, specialItemId)
                }
            }
            return
        }

        if (!isBypassing(player)) {
            event.cancel()
        }
    }

    @EventHandler
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        if (HideAndSeekRoleManager.roleOf(player) != SeekerRole) return

        val heldItem = player.inventory.getItem(event.newSlot)
        plugin.launch {
            HideAndSeekItems.updateShrinkScale(player, heldItem)
        }
    }

    @EventHandler
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        val context = currentContext() ?: return
        if (player.uniqueId !in context.allEventPlayers) return
        if (!isBypassing(player)) {
            event.cancel()
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!event.hasChangedBlock()) return
        if (!HideAndSeekService.isRolePhase) return
        if (event.to.block.type != Material.WATER) return

        val player = event.player
        val role = HideAndSeekRoleManager.roleOf(player)
        if (role != SeekerRole && role != HiderRole) return

        player.velocity = player.location.direction.multiply(-1).setY(0.5)
        player.damage(5.0)
    }
}
