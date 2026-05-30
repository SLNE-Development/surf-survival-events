package dev.slne.surf.survival.events.base.service

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.game.GameKey
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object AnnouncementService {

    fun broadcastOpenEvent() {
        val activeGameKey = GameService.getActiveGameKey()
        server.sendMessage(createOpenEventMessage(activeGameKey))
    }

    fun sendOpenEvent(player: Player) {
        val activeGameKey = GameService.getActiveGameKey()
        //player.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f)

        player.sendMessage(createOpenEventMessage(activeGameKey))
    }

    private fun createOpenEventMessage(key: GameKey<*>) = buildText {
        text("--------------------------------------------------", TextColor.color(0x599542))
        appendNewline()
        appendNewline()
        text("${key.displayName} wurde gestartet!", TextColor.color(0xD98E8D))
        appendNewline()
        info("Gehe zum Spawn und klicke auf Arty, um teilzunehmen.")
        appendNewline()
        appendNewline()
        text("--------------------------------------------------", TextColor.color(0x599542))
    }

    fun broadcastCloseEvent() {
        val activeGame = GameService.getActiveGameKey()
        server.sendMessage(createCloseEventMessage(activeGame))
    }

    fun sendCloseEvent(player: Player) {
        val activeGame = GameService.getActiveGameKey()
        player.sendMessage(createCloseEventMessage(activeGame))
    }

    private fun createCloseEventMessage(key: GameKey<*>) = buildText {
        text("--------------------------------------------------", TextColor.color(0x599542))
        appendNewline()
        appendNewline()
        error("Das ${key.displayName} wurde abgebrochen!")
        appendNewline()
        appendNewline()
        text("--------------------------------------------------", TextColor.color(0x599542))
    }
}