package dev.slne.surf.survival.events.base.listeners

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.NpcService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object NpcInteractListener : Listener {
    @EventHandler
    fun onNpcInteract(event: NpcInteractEvent) {
        val player = event.player
        if (event.npc.uniqueName != NpcService.SURVIVAL_EVENTS_NPC_NAME) {
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

        if (GameService.isQueued(player)) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist bereits in der Queue.")
            }
            return
        }

        val joined = GameService.joinQueue(player)

        if (joined) {
            player.sendText {
                appendSuccessPrefix()
                success("Du bist jetzt in der Queue.")
            }
        } else {
            player.sendText {
                appendInfoPrefix()
                info("Du befindest dich bereits in der Queue.")
            }
        }
    }
}