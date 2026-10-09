package dev.slne.surf.survival.events.werewolf.domain.roleActions

import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import java.util.*

object DoctorActions {

    fun isValid(
        action: NightAction.DoctorProtect,
        players: Map<UUID, WerewolfPlayer>,
    ): Boolean {
        return players[action.target]?.isAlive == true
    }

    fun resolveTarget(actions: List<NightAction>): UUID? = actions
        .filterIsInstance<NightAction.DoctorProtect>()
        .lastOrNull()
        ?.target
}
