package dev.slne.surf.survival.events.base.service

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.game.GameKey
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

internal object AnnouncementService {

    fun broadcastStartedEvent(context: GameContext) {
        server.sendMessage(createStartedEventMessage(context))
    }

    fun broadcastRunningEvent(key: GameKey<*> = GameService.getActiveGameKey()) {
        server.sendMessage(createRunningEventMessage(key))
    }

    fun sendRunningEvent(player: Player, key: GameKey<*> = GameService.getActiveGameKey()) {
        player.sendMessage(createRunningEventMessage(key))
    }

    fun broadcastCloseEvent(key: GameKey<*>) {
        server.sendMessage(createCloseEventMessage(key))
    }

    private fun createStartedEventMessage(context: GameContext) = buildText {
        text("--------------------------------------------------", TextColor.color(0x599542))
        appendNewline()
        appendNewline()
        text("${context.key.displayName} wurde gestartet!", TextColor.color(0xD98E8D))
        appendNewline()
        info("Aktive Spieler:")
        appendSpace()
        variableValue(context.activePlayerCount.toString())
        if (context.reservePlayerCount > 0) {
            appendSpace()
            info("| Reserve:")
            appendSpace()
            variableValue(context.reservePlayerCount.toString())
        }
        if (context.spectatorCount > 0) {
            appendSpace()
            info("| Zuschauer:")
            appendSpace()
            variableValue(context.spectatorCount.toString())
        }
        appendNewline()
        appendNewline()
        text("--------------------------------------------------", TextColor.color(0x599542))
    }

    private fun createRunningEventMessage(key: GameKey<*>) = buildText {
        text("--------------------------------------------------", TextColor.color(0x599542))
        appendNewline()
        appendNewline()
        text("${key.displayName} läuft bereits!", TextColor.color(0xD98E8D))
        appendNewline()
        info("Je nach Event wirst du automatisch als Spieler oder Zuschauer hinzugefügt.")
        appendNewline()
        appendNewline()
        text("--------------------------------------------------", TextColor.color(0x599542))
    }

    private fun createCloseEventMessage(key: GameKey<*>) = buildText {
        text("--------------------------------------------------", TextColor.color(0x599542))
        appendNewline()
        appendNewline()
        error("Das ${key.displayName} wurde gestoppt!")
        appendNewline()
        appendNewline()
        text("--------------------------------------------------", TextColor.color(0x599542))
    }
}