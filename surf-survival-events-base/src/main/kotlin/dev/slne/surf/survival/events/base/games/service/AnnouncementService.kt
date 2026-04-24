package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.api.core.messages.adventure.sendText
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object AnnouncementService {
    fun sendAnnouncement(player: Player) {
        if (!GameService.isGameActive()) {
            return
        }

        val activeGame = GameService.getActiveGame()

        player.sendText {
            text("--------------------------------------------------\\n", TextColor.color(0x599542))
            variableValue("Das ${activeGame.displayName} wurde gestartet!")

            text("--------------------------------------------------\\n", TextColor.color(0x599542))
        }
    }
}