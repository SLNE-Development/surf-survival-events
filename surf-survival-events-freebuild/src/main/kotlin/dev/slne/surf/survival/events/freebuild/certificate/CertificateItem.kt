package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.meta
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView

object CertificateItem {
    /**
     * The name is set as plain item name (not a custom name), so it is not italic and takes the purple colour of
     * the epic rarity.
     */
    fun create(map: MapView, record: CertificateRecord): ItemStack = buildItem(Material.FILLED_MAP) {
        meta<MapMeta> { mapView = map }
        setData(DataComponentTypes.ITEM_NAME, Component.text("Teilnahmeurkunde – ${record.eventName}"))
        setData(DataComponentTypes.RARITY, ItemRarity.EPIC)
        buildLore {
            line {
                spacer("Spieler: ")
                variableValue(record.playerName)
            }
            line {
                spacer("Datum: ")
                variableValue(CertificateDates.format(record.date))
            }
        }
    }
}
