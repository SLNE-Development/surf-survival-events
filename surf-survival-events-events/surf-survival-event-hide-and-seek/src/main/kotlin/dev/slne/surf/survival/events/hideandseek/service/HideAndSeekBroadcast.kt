package dev.slne.surf.survival.events.hideandseek.service

import dev.slne.surf.api.core.messages.adventure.TitleBuilder
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.sound.Sound
import org.bukkit.Sound as BukkitSound

object HideAndSeekBroadcast {

    fun broadcast(message: SurfComponentBuilder.() -> Unit) {
        currentContext()?.onlineEventPlayers?.forEach { player ->
            player.sendText(message)
        }
    }

    fun broadcastTitle(title: TitleBuilder.() -> Unit) {
        currentContext()?.onlineEventPlayers?.forEach { player ->
            player.showTitle(title)
        }
    }

    fun broadcastSound(sound: BukkitSound, volume: Float, pitch: Float) {
        currentContext()?.onlineEventPlayers?.forEach { player ->
            player.playSound {
                type(sound)
                volume(volume)
                pitch(pitch)
                source(Sound.Source.HOSTILE)
            }
        }
    }
}
