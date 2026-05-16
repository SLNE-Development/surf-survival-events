package dev.slne.surf.survival.events.werewolf.domain.roleActions

import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import java.util.*

object DoctorActions {

    fun isValid(
        action: NightAction.DoctorProtect,
        players: Map<UUID, WerewolfPlayer>,
        werewolfTarget: UUID?,
    ): Boolean {
        if (players[action.target]?.isAlive != true) return false

        return action.target == action.actor || action.target == werewolfTarget
    }

    fun resolveTarget(actions: List<NightAction>): UUID? {
        return actions
            .filterIsInstance<NightAction.DoctorProtect>()
            .lastOrNull()
            ?.target
    }
}
