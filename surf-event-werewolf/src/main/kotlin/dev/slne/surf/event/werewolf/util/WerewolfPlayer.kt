package dev.slne.surf.event.werewolf.util

import dev.slne.surf.api.paper.extensions.server
import java.util.UUID

data class WerewolfPlayer(
    val uuid: UUID,
    var role: WerwolfRoles = WerwolfRoles.VILLAGER,
    var isAlive: Boolean = true,
    var inLoveWith: UUID? = null,
    val votes: MutableSet<UUID> = mutableSetOf(),
    var chosenVictim: UUID? = null
) {
    val name: String
        get() = server.getPlayer(uuid)?.name ?: "Unbekannt"
}
