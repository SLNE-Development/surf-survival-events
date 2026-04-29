package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.api.core.messages.adventure.clickRunsCommand
import dev.slne.surf.api.core.messages.adventure.sendText
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object AnnouncementService {
    fun sendOpenEvent(player: Player){
        val activeGame = GameService.getActiveGame()

        player.sendText {
            text("--------------------------------------------------", TextColor.color(0x599542))
            appendNewline()
            appendNewline()
            text("Das ${activeGame.displayName} hat gestartet!", TextColor.color(0xD98E8D))
            appendNewline()
            info("Trete über")
            appendSpace()
            text("/event join", TextColor.color(0xF5A19F))
            clickRunsCommand("/event join")
            appendSpace()
            info("dem Event bei.")
            appendNewline()
            appendNewline()
            text("--------------------------------------------------", TextColor.color(0x599542))
        }
    }

    fun sendCloseEvent(player: Player){
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