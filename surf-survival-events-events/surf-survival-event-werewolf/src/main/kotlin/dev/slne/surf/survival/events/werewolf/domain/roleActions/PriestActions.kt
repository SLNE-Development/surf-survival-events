package dev.slne.surf.survival.events.werewolf.domain.roleActions

import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import java.util.*

data class PriestResolution(
    val hitWerewolf: Boolean,
    val eliminatedPlayers: List<UUID>
)

object PriestActions {

    fun isValid(actor: WerewolfPlayer, target: WerewolfPlayer): Boolean {
        return actor.isAlive &&
                target.isAlive &&
                actor.uuid != target.uuid
    }

    fun resolve(
        actor: UUID,
        target: UUID,
        players: Map<UUID, WerewolfPlayer>
    ): PriestResolution {
        return if (players[target]?.role == WerwolfRoles.WERWOLF) {
            PriestResolution(
                hitWerewolf = true,
                eliminatedPlayers = listOf(target)
            )
        } else {
            PriestResolution(
                hitWerewolf = false,
                eliminatedPlayers = listOf(actor)
            )
        }
    }
}
