package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.freebuild.plugin
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Tells players about their pending certificates with a clickable chat message that claims the certificate,
 * together with a sound.
 *
 * The message is sent when a certificate is issued to an online player and again once per login until the
 * certificate is claimed. Click callbacks only live in memory, so after a restart the next login brings a fresh one.
 * They last a week, so the link keeps working for players that stay online for a long time.
 */
object CertificateDelivery : Listener {
    private val remindedThisLogin = ConcurrentHashMap.newKeySet<UUID>()

    private val claimCallbackOptions = ClickCallback.Options.builder()
        .uses(ClickCallback.UNLIMITED_USES)
        .lifetime(Duration.ofDays(7))
        .build()

    private val notificationSound = Sound.sound(
        Key.key("ui.toast.challenge_complete"),
        Sound.Source.MASTER,
        0.8f,
        1f
    )

    fun notifyIfOnline(record: CertificateRecord) {
        val player = Bukkit.getPlayer(record.playerUuid) ?: return
        player.scheduler.run(plugin, { notify(player, listOf(record)) }, null)
    }

    /** Sends one message per certificate and plays the sound once. Must run on the thread of [player]. */
    private fun notify(player: Player, records: List<CertificateRecord>) {
        if (records.isEmpty()) {
            return
        }

        records.forEach { sendClaimMessage(player, it) }
        player.playSound(notificationSound)
    }

    private fun sendClaimMessage(player: Player, record: CertificateRecord) {
        val mapId = record.mapId
        val claimCallback: ClickCallback<Audience> =
            ClickCallback.widen(ClickCallback<Player> { claim(it, mapId) }, Player::class.java)

        player.sendText {
            appendNewline()
            appendInfoPrefix()
            info("Du hast eine Teilnahmeurkunde für ")
            variableValue(record.eventName)
            info(" erhalten. Vielen Dank fürs Mitmachen! ")
            append {
                success("[ABHOLEN]")
                hoverEvent(buildText {
                    info("Klicke, um die Urkunde in dein Inventar zu legen.")
                })
                clickEvent(ClickEvent.callback(claimCallback, claimCallbackOptions))
            }
            appendNewline()
        }
    }

    private fun claim(player: Player, mapId: Int) {
        when (val result = CertificateService.claim(player, mapId)) {
            is ClaimResult.Claimed -> player.sendText {
                appendSuccessPrefix()
                success("Du hast die Teilnahmeurkunde für ")
                variableValue(result.record.eventName)
                success(" abgeholt.")
            }

            ClaimResult.InventoryFull -> player.sendText {
                appendErrorPrefix()
                error("Dein Inventar ist voll. Mach einen Platz frei und klicke dann erneut auf die Nachricht.")
            }

            ClaimResult.AlreadyClaimed -> player.sendText {
                appendErrorPrefix()
                error("Du hast diese Teilnahmeurkunde bereits abgeholt.")
            }

            ClaimResult.NotFound -> player.sendText {
                appendErrorPrefix()
                error("Diese Teilnahmeurkunde konnte nicht gefunden werden.")
            }

            ClaimResult.InProgress -> Unit
        }
    }

    @EventHandler
    fun onClientLoadedWorld(event: PlayerClientLoadedWorldEvent) {
        val player = event.player
        if (!remindedThisLogin.add(player.uniqueId)) {
            return
        }

        notify(player, CertificateService.pendingFor(player.uniqueId))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        remindedThisLogin.remove(event.player.uniqueId)
    }
}
