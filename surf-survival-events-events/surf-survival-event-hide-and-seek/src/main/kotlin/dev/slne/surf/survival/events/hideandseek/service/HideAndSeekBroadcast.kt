package dev.slne.surf.survival.events.hideandseek.service

import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekTheme.ThemeSound
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

/** Sends themed messages, titles and sounds to everyone in the running hide and seek event. */
object HideAndSeekBroadcast {

    private fun recipients(): List<Player> = currentContext()?.onlineEventPlayers ?: emptyList()

    /** Sends a themed chat message with the theme prefix. */
    fun broadcast(text: String, vararg placeholders: TagResolver) {
        recipients().forEach { player -> HideAndSeekThemes.send(player, text, *placeholders) }
    }

    /** Sends an already rendered message with the theme prefix. */
    fun broadcastRaw(message: Component) {
        recipients().forEach { player -> HideAndSeekThemes.sendRaw(player, message) }
    }

    /** Sends themed chat lines without a prefix, e.g. a multi-line announcement. */
    fun broadcastLines(lines: List<String>) {
        val textColor = HideAndSeekThemes.color(HideAndSeekThemes.theme.textColor)
        val rendered = lines.map { HideAndSeekThemes.render(it).colorIfAbsent(textColor) }
        recipients().forEach { player -> rendered.forEach(player::sendMessage) }
    }

    fun broadcastTitle(title: String, subtitle: String) {
        val shown = Title.title(
            HideAndSeekThemes.render(title),
            HideAndSeekThemes.render(subtitle),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2500), Duration.ofMillis(1000))
        )
        recipients().forEach { player -> player.showTitle(shown) }
    }

    fun broadcastSound(sound: ThemeSound) {
        recipients().forEach { player -> HideAndSeekThemes.playSound(player, sound) }
    }
}
