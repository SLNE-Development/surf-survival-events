package dev.slne.surf.survival.events.base.service

import dev.slne.surf.api.core.messages.adventure.sendText
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object AnnouncementService {
    fun sendOpenEvent(player: Player) {
        val activeGame = GameService.getActiveGame()
        //player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f)


        player.sendText {
            text("--------------------------------------------------", TextColor.color(0x599542))
            appendNewline()
            appendNewline()
            text("${activeGame.displayName} wurde gestartet!", TextColor.color(0xD98E8D))
            appendNewline()
            info("Gehe zum Central Spawn und klicke auf Arty, um teilzunehmen.")
            appendNewline()
            appendNewline()
            text("--------------------------------------------------", TextColor.color(0x599542))
        }
    }

    fun sendCloseEvent(player: Player) {
        val activeGame = GameService.getActiveGame()
        player.sendText {
            text("--------------------------------------------------", TextColor.color(0x599542))
            appendNewline()
            appendNewline()
            error("Das ${activeGame.displayName} wurde abgebrochen!")
            appendNewline()
            appendNewline()
            text("--------------------------------------------------", TextColor.color(0x599542))

        }
    }
}