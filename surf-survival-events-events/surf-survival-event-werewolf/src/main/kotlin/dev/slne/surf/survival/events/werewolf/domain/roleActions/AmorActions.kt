package dev.slne.surf.event.werewolf.domain.roleActions

import dev.slne.surf.event.werewolf.util.NightAction
import dev.slne.surf.event.werewolf.util.WerewolfPlayer
import java.util.*

object AmorActions {

    fun isValid(
        action: NightAction.AmorLink,
        players: Map<UUID, WerewolfPlayer>,
        dayNumber: Int,
    ): Boolean {
        return dayNumber == 1 &&
                action.first != action.second &&
                players[action.first]?.isAlive == true &&
                players[action.second]?.isAlive == true
    }

    fun resolve(actions: List<NightAction>): Pair<UUID, UUID>? {
        val amorLink = actions
            .filterIsInstance<NightAction.AmorLink>()
            .lastOrNull()
            ?: return null

        return amorLink.first to amorLink.second
    }

    fun apply(players: MutableMap<UUID, WerewolfPlayer>, lovers: Pair<UUID, UUID>?) {
        if (lovers == null) return

        val (firstId, secondId) = lovers
        val firstPlayer = players[firstId] ?: return
        val secondPlayer = players[secondId] ?: return

        firstPlayer.inLoveWith = secondId
        secondPlayer.inLoveWith = firstId
    }
}
