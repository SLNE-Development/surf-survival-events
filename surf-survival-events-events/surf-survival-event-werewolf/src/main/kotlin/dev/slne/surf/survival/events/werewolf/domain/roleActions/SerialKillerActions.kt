package dev.slne.surf.event.werewolf.domain.roleActions

import dev.slne.surf.event.werewolf.util.NightAction
import dev.slne.surf.event.werewolf.util.WerewolfPlayer
import java.util.*

object SerialKillerActions {

    fun isValid(
        action: NightAction.SerialKillerKill,
        players: Map<UUID, WerewolfPlayer>,
    ): Boolean {
        return players[action.target]?.isAlive == true &&
                action.actor != action.target
    }

    fun resolveTarget(actions: List<NightAction>): UUID? {
        return actions
            .filterIsInstance<NightAction.SerialKillerKill>()
            .lastOrNull()
            ?.target
    }
}
