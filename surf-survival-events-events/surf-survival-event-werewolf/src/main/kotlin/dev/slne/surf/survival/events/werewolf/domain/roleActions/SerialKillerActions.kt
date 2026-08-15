package dev.slne.surf.survival.events.werewolf.domain.roleActions

import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import java.util.*

object SerialKillerActions {

    fun isValid(
        action: NightAction.SerialKillerKill,
        players: Map<UUID, WerewolfPlayer>,
    ): Boolean {
        return players[action.target]?.isAlive == true &&
                action.actor != action.target
    }

    fun resolveTarget(actions: List<NightAction>): UUID? = actions
        .filterIsInstance<NightAction.SerialKillerKill>()
        .lastOrNull()
        ?.target
}
