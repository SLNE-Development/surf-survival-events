package dev.slne.surf.survival.events.base.npc.listener

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.survival.events.base.games.service.GameService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object NpcInteractListener : Listener {
    @EventHandler
    fun onNpcInteract(event: NpcInteractEvent) {
        val player = event.player
        if (event.npc.uniqueName != "survival_events") {
            return
        }

        if (!GameService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Es ist derzeit kein Event aktiv.")
            }
            return
        }

        if (GameService.isInGameQueue(player) || GameService.isInWaitingQueue(player)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits in der Warteschlange.")
            }
            return
        }


        GameService.joinWaitingQueue(player)
        player.sendText {
            appendInfoPrefix()
            info("Die Warteschlange ist voll. Du befindest dich nun auf der Ersatzbank.")
        }
    }
}