package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.freebuild.permission.PermissionList
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import java.time.Duration

/**
 * Hands out copies of existing certificates to team members with the copy permission, for example to hang a
 * certificate up somewhere. A copy shows the same map, keeps the owner in its lore and does not change the claim of
 * the owner.
 */
object CertificateCopies {
    private val copyCallbackOptions = ClickCallback.Options.builder()
        .uses(ClickCallback.UNLIMITED_USES)
        .lifetime(Duration.ofHours(1))
        .build()

    /** Gives [player] a copy of [record] or tells them that their inventory is full. Must run on their thread. */
    fun give(player: Player, record: CertificateRecord) {
        if (!CertificateService.giveCopy(player, record)) {
            player.sendText {
                appendErrorPrefix()
                error("Dein Inventar ist voll. Mach einen Platz frei und versuche es erneut.")
            }
            return
        }

        player.sendText {
            appendSuccessPrefix()
            success("Du hast eine Kopie der Teilnahmeurkunde von ")
            variableValue(record.playerName)
            success(" für ")
            variableValue(record.eventName)
            success(" erhalten.")
        }
    }

    /** Lists [certificates] of [ownerName], each with a button that gives the clicking player a copy. */
    fun sendList(player: Player, ownerName: String, certificates: List<CertificateRecord>) {
        player.sendText {
            appendInfoPrefix()
            info("Teilnahmeurkunden von ")
            variableValue(ownerName)
            info(":")
            for (record in certificates) {
                appendNewline()
                spacer("- ")
                variableValue(record.eventName)
                spacer(" (${CertificateDates.format(record.date)}, ")
                spacer(if (record.isClaimed) "abgeholt" else "noch nicht abgeholt")
                spacer(") ")
                append(button(record.mapId))
            }
        }
    }

    /** A "[Kopie nehmen]" button for certificate [mapId], usable for an hour by players with the copy permission. */
    fun button(mapId: Int): Component {
        val callback: ClickCallback<Audience> =
            ClickCallback.widen(ClickCallback<Player> { take(it, mapId) }, Player::class.java)

        return buildText {
            success("[Kopie nehmen]")
            hoverEvent(buildText { info("Klicke, um eine Kopie in dein Inventar zu legen.") })
            clickEvent(ClickEvent.callback(callback, copyCallbackOptions))
        }
    }

    private fun take(player: Player, mapId: Int) {
        if (!player.hasPermission(PermissionList.COMMAND_CERTIFICATE_COPY)) {
            player.sendText {
                appendErrorPrefix()
                error("Du darfst keine Teilnahmeurkunden kopieren.")
            }
            return
        }

        val record = CertificateService.recordOf(mapId)
        if (record == null) {
            player.sendText {
                appendErrorPrefix()
                error("Diese Teilnahmeurkunde konnte nicht gefunden werden.")
            }
            return
        }

        give(player, record)
    }
}
