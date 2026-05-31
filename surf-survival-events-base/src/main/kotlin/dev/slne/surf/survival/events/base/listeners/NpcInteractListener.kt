package dev.slne.surf.survival.events.base.listeners

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.survival.events.base.service.GameService
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
                error("Derzeit ist kein Event aktiv.")
            }
            return
        }

        if (GameService.isSpectator(player.uniqueId)) {
            player.sendText {
                appendErrorPrefix()
                error("Das ist nicht kosmetisch. Du bist bereits Zuschauer des Events.")
            }
            return
        }

        if (GameService.isInGameQueue(player) || GameService.isInWaitingQueue(player)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits in der Queue.")
            }
            return
        }


        GameService.joinWaitingQueue(player)
        player.sendText {
            appendInfoPrefix()
            info("Die Queue ist voll. Du befindest dich nun auf der Ersatzbank.")
        }
    }
}