package dev.slne.surf.survival.events.hideandseek.util

import dev.slne.surf.api.core.messages.adventure.playSound
import kotlinx.coroutines.future.await
import net.kyori.adventure.sound.Sound
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.Sound as BukkitSound

suspend fun Player.tp(location: Location, playSound: Boolean = true) {
    teleportAsync(location).await()
    if (playSound) {
        playSound {
            type(BukkitSound.ENTITY_ENDERMAN_TELEPORT)
            volume(.5f)
            source(Sound.Source.HOSTILE)
        }
    }
}

fun formatClock(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun formatLongDuration(totalSeconds: Long): String {
    return if (totalSeconds >= 60 && totalSeconds % 60 == 0L) {
        val minutes = totalSeconds / 60
        if (minutes == 1L) "1 Minute" else "$minutes Minuten"
    } else {
        if (totalSeconds == 1L) "1 Sekunde" else "$totalSeconds Sekunden"
    }
}
