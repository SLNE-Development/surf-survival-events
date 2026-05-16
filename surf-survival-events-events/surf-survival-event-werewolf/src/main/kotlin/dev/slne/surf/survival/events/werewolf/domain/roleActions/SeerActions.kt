package dev.slne.surf.survival.events.werewolf.domain.roleActions

import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import java.util.*

object SeerActions {
    fun isValid(
        action: NightAction.SeerInspect,
        players: Map<UUID, WerewolfPlayer>,
    ): Boolean {
        return players[action.target]?.isAlive == true &&
                action.actor != action.target
    }

    fun inspectTarget(
        action: NightAction.SeerInspect,
        players: Map<UUID, WerewolfPlayer>,
    ): WerwolfRoles? {
        return players[action.target]?.role
    }
}
