package dev.slne.surf.survival.events.red.light.green.light.command.util

import dev.slne.surf.api.core.messages.adventure.playSound
import org.bukkit.Sound
import org.bukkit.entity.Player

object AreaSelectionSounds {
    fun playActivated(player: Player) = player.playSound(true) {
        type(Sound.BLOCK_NOTE_BLOCK_PLING)
        volume(1f)
        pitch(1.5f)
    }

    fun playDeactivated(player: Player) = player.playSound(true) {
        type(Sound.BLOCK_NOTE_BLOCK_BASS)
        volume(1f)
        pitch(0.7f)
    }

    fun playPositionSet(player: Player) = player.playSound(true) {
        type(Sound.UI_BUTTON_CLICK)
        volume(1f)
        pitch(1f)
    }

    fun playAreaSaved(player: Player) = player.playSound(true) {
        type(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)
        volume(1f)
        pitch(1f)
    }
}