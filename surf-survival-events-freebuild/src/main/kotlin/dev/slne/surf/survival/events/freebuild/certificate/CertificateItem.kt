package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import dev.slne.surf.survival.events.freebuild.plugin
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.MapId
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object CertificateItem {
    /** Marks certificate items with their map id; [CertificateLore] shows the lore for items carrying it. */
    val key by lazy { NamespacedKey(plugin, "certificate") }

    /**
     * Only the map id is set, so the map does not have to be loaded for this. The name is set as plain item name
     * (not a custom name), so it is not italic and takes the purple colour of the epic rarity. The tooltip of the map
     * id component, which shows the map number and "Locked", is hidden. The lore is not stored on the item but added
     * by [CertificateLore].
     *
     * If `certificate-item-model` is configured, the item uses that model from the resource pack as icon. Item frames
     * and the hand still show the map, since they render items by their map id.
     */
    fun create(record: CertificateRecord): ItemStack = buildItem(Material.FILLED_MAP) {
        setData(DataComponentTypes.MAP_ID, MapId.mapId(record.mapId))
        setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.MAP_ID).build()
        )
        setData(DataComponentTypes.ITEM_NAME, Component.text("Teilnahmeurkunde – ${record.eventName}"))
        setData(DataComponentTypes.RARITY, ItemRarity.EPIC)
        configuredItemModel()?.let { setData(DataComponentTypes.ITEM_MODEL, it) }
        editPersistentDataContainer { it.set(key, PersistentDataType.INTEGER, record.mapId) }
    }

    private fun configuredItemModel(): Key? {
        val model = FreebuildPartConfig.getConfig().certificateItemModel.trim()
        if (model.isEmpty()) {
            return null
        }

        return try {
            Key.key(model)
        } catch (_: InvalidKeyException) {
            plugin.logger.warning("Ignoring invalid certificate-item-model '$model' in config.yml")
            null
        }
    }
}
