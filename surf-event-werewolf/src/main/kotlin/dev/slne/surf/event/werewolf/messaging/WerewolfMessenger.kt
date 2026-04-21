package dev.slne.surf.event.werewolf.messaging

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.event.werewolf.service.WerewolfService
import dev.slne.surf.event.werewolf.util.WerwolfRoles
import toBukkitPlayer

class WerewolfMessenger(private val service: WerewolfService) {

    fun announceToAll(content: SurfComponentBuilder.() -> Unit) {
        service.players.keys.forEach { uuid ->
            uuid.toBukkitPlayer()?.sendText(content)
        }

        service.leader?.toBukkitPlayer()?.sendText(content)
    }

    fun announceToLeader(content: SurfComponentBuilder.() -> Unit) {
        service.leader?.toBukkitPlayer()?.sendText(content)
    }

    fun announceToRole(
        role: WerwolfRoles,
        onlyAlive: Boolean = true,
        content: SurfComponentBuilder.() -> Unit
    ) {
        service.players.values
            .filter { it.role == role }
            .filter { !onlyAlive || it.isAlive }
            .forEach { player ->
                player.uuid.toBukkitPlayer()?.sendText(content)
            }
    }

    fun announceToAlive(content: SurfComponentBuilder.() -> Unit) {
        service.getAlivePlayers()
            .forEach { player ->
                player.uuid.toBukkitPlayer()?.sendText(content)
            }
    }
}
