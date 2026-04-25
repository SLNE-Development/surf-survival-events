package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.api.core.messages.adventure.clickRunsCommand
import dev.slne.surf.api.core.messages.adventure.sendText
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object AnnouncementService {
    fun sendOpenEvent(player: Player){
        val activeGame = GameService.getActiveGame()

        player.sendText {
            text("--------------------------------------------------\n", TextColor.color(0x599542))
            text("\n")
            text("Das ${activeGame.displayName} hat gestartet! \n", TextColor.color(0xD98E8D))
            info("Trete über")
            appendSpace()
            text("/event join", TextColor.color(0xF5A19F))
            clickRunsCommand("/event join")
            appendSpace()
            info("dem Event bei.\n")
            text("\n")
            text("--------------------------------------------------", TextColor.color(0x599542))
        }
    }

    fun sendCloseEvent(player: Player){
        val activeGame = GameService.getActiveGame()
        player.sendText {
            text("--------------------------------------------------\n", TextColor.color(0x599542))
            text("\n")
            warning("Das ${activeGame.displayName} wurde Abgebrochen! \n")
            text("\n")
            text("--------------------------------------------------", TextColor.color(0x599542))

        }
    }
}