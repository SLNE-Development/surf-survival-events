package dev.slne.surf.survival.events.freebuild.certificate

import com.destroystokyo.paper.event.inventory.PrepareResultEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.papermc.paper.event.player.PlayerPickItemEvent
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.CartographyInventory
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.test.Test

class CertificateCopyProtectionTest {
    private val certificate = mockk<ItemStack>()
    private val otherMap = mockk<ItemStack>()
    private val protection = CertificateCopyProtection { it === certificate }

    private fun cartography(first: ItemStack?, second: ItemStack? = null) = mockk<CartographyInventory> {
        every { getItem(0) } returns first
        every { getItem(1) } returns second
    }

    private fun anvil(first: ItemStack?) = mockk<AnvilInventory> {
        every { getItem(0) } returns first
        every { getItem(1) } returns null
    }

    @Test
    fun `the cartography table offers no copy of a certificate`() {
        val event = mockk<PrepareResultEvent>(relaxed = true) { every { inventory } returns cartography(certificate) }

        protection.onPrepareResult(event)

        verify { event.result = null }
    }

    @Test
    fun `a certificate in the second cartography slot is protected as well`() {
        val event = mockk<PrepareResultEvent>(relaxed = true) {
            every { inventory } returns cartography(otherMap, certificate)
        }

        protection.onPrepareResult(event)

        verify { event.result = null }
    }

    @Test
    fun `the anvil does not rename a certificate`() {
        val event = mockk<PrepareAnvilEvent>(relaxed = true) { every { inventory } returns anvil(certificate) }

        protection.onPrepareResult(event)

        verify { event.result = null }
    }

    @Test
    fun `other maps are copied as usual`() {
        val event = mockk<PrepareResultEvent>(relaxed = true) { every { inventory } returns cartography(otherMap) }

        protection.onPrepareResult(event)

        verify(exactly = 0) { event.result = any() }
    }

    @Test
    fun `other inventories are not touched`() {
        val event = mockk<PrepareResultEvent>(relaxed = true) {
            every { inventory } returns mockk<Inventory> { every { getItem(any()) } returns certificate }
        }

        protection.onPrepareResult(event)

        verify(exactly = 0) { event.result = any() }
    }

    @Test
    fun `taking the result next to a certificate is refused`() {
        val event = mockk<InventoryClickEvent>(relaxed = true) {
            every { rawSlot } returns 2
            every { inventory } returns cartography(certificate)
        }

        protection.onResultClick(event)

        verify { event.isCancelled = true }
    }

    @Test
    fun `clicking the certificate itself is allowed`() {
        val event = mockk<InventoryClickEvent>(relaxed = true) {
            every { rawSlot } returns 0
            every { inventory } returns cartography(certificate)
        }

        protection.onResultClick(event)

        verify(exactly = 0) { event.isCancelled = true }
    }

    @Test
    fun `the crafting table does not clone a certificate`() {
        val crafting = mockk<CraftingInventory>(relaxed = true) {
            every { matrix } returns arrayOf(null, certificate, otherMap)
        }
        val event = mockk<PrepareItemCraftEvent> { every { inventory } returns crafting }

        protection.onPrepareCraft(event)

        verify { crafting.result = null }
    }

    @Test
    fun `the crafter does not clone a certificate`() {
        val event = mockk<CrafterCraftEvent>(relaxed = true) { every { result } returns certificate }

        protection.onCrafterCraft(event)

        verify { event.isCancelled = true }
    }

    @Test
    fun `picking a framed certificate in creative mode creates no copy`() {
        val event = mockk<PlayerPickItemEvent>(relaxed = true) {
            every { item } returns certificate
            every { sourceSlot } returns -1
        }

        protection.onPickItem(event)

        verify { event.isCancelled = true }
    }

    @Test
    fun `picking selects a certificate that is already in the inventory`() {
        val event = mockk<PlayerPickItemEvent>(relaxed = true) {
            every { item } returns certificate
            every { sourceSlot } returns 4
        }

        protection.onPickItem(event)

        verify(exactly = 0) { event.isCancelled = true }
    }
}
