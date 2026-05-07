package dev.slne.surf.event.werewolf.domain.roleActions

import dev.slne.surf.event.werewolf.util.NightAction
import dev.slne.surf.event.werewolf.util.WerewolfPlayer
import dev.slne.surf.event.werewolf.util.WerwolfRoles
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
