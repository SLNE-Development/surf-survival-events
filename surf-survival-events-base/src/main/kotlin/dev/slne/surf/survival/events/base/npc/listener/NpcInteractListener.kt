package dev.slne.surf.survival.events.base.npc.listener

import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.survival.events.base.games.service.GameService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object NpcInteractListener : Listener {
    @EventHandler
    fun onNpcInteract(event: NpcInteractEvent) {
        if (!event.npc.uniqueName.startsWith("survival_events")) {
            return
        }
        event.player.performCommand("event join")
        GameService.joinWaitingQueue(event.player)
    }
}