package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Shows the lore of certificate items. It is added when the item is sent to a client and never stored on the item,
 * so changes to the text apply to all certificates, including those that were already claimed.
 */
object CertificateLore : SurfPaperPacketLoreHandler {
    override fun handleLore(
        loreToDisplay: MutableList<Component>,
        pdc: PersistentDataContainerView,
        itemStack: ItemStack
    ) {
        val mapId = pdc.get(CertificateItem.key, PersistentDataType.INTEGER) ?: return
        val record = CertificateService.recordOf(mapId) ?: return

        loreToDisplay += lines(record)
    }

    fun lines(record: CertificateRecord): List<Component> = listOf(
        Component.empty(),
        buildText {
            spacer("Ausgestellt für: ")
            variableValue(record.playerName)
        },
        buildText {
            spacer("Teilgenommen am: ")
            variableValue(CertificateDates.format(record.date))
        },
        Component.empty(),
        buildText {
            darkSpacer("Persönliche, kopiergeschützte Urkunde")
        },
    ).map { it.decoration(TextDecoration.ITALIC, false) }
}
