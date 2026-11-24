package dev.slne.surf.survival.events.freebuild.certificate

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.CartographyInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Prevents copying and renaming certificates, for everyone including their owner: in the cartography table, in the
 * crafting table (map cloning), in the crafter, in the anvil and by picking a framed certificate in creative mode.
 *
 * The creative inventory itself cannot be protected, since the client sends the items it puts there.
 */
class CertificateCopyProtection(private val isCertificate: (ItemStack?) -> Boolean) : Listener {
    @EventHandler
    fun onPrepareResult(event: PrepareResultEvent) {
        if (isBlocked(event.inventory)) {
            event.result = null
        }
    }

    @EventHandler
    fun onResultClick(event: InventoryClickEvent) {
        if (event.rawSlot == RESULT_SLOT && isBlocked(event.inventory)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory

        if (inventory.matrix.any(isCertificate)) {
            inventory.result = null
        }
    }

    @EventHandler
    fun onCrafterCraft(event: CrafterCraftEvent) {
        if (isCertificate(event.result)) {
            event.isCancelled = true
        }
    }

    /** Without a matching item in the inventory, picking would add a new copy for players in creative mode. */
    @EventHandler
    fun onPickItem(event: PlayerPickItemEvent) {
        if (event.sourceSlot == NOT_IN_INVENTORY && isCertificate(event.item)) {
            event.isCancelled = true
        }
    }

    private fun isBlocked(inventory: Inventory): Boolean {
        if (inventory !is CartographyInventory && inventory !is AnvilInventory) {
            return false
        }

        return isCertificate(inventory.getItem(0)) || isCertificate(inventory.getItem(1))
    }

    private companion object {
        const val RESULT_SLOT = 2
        const val NOT_IN_INVENTORY = -1
    }
}
