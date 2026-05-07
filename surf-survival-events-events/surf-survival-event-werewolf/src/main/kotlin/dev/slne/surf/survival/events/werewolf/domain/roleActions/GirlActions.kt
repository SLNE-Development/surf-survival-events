package dev.slne.surf.event.werewolf.domain.roleActions

import dev.slne.surf.event.werewolf.util.GirlPeekOutcome
import dev.slne.surf.event.werewolf.util.NightAction
import dev.slne.surf.event.werewolf.util.WerewolfPlayer
import dev.slne.surf.event.werewolf.util.WerwolfRoles
import java.util.*
import kotlin.random.Random

object GirlActions {

    fun isValid(
        action: NightAction.GirlPeek,
        players: Map<UUID, WerewolfPlayer>,
    ): Boolean {
        return when (val outcome = action.outcome) {
            GirlPeekOutcome.TooDark,
            GirlPeekOutcome.CaughtByWerewolves -> true

            is GirlPeekOutcome.FoundWerewolf -> {
                val target = players[outcome.target]
                target?.isAlive == true && target.role == WerwolfRoles.WERWOLF
            }
        }
    }

    fun rollOutcome(players: Map<UUID, WerewolfPlayer>): GirlPeekOutcome {
        val aliveWerewolves = players.values
            .filter { it.isAlive && it.role == WerwolfRoles.WERWOLF }

        if (aliveWerewolves.isEmpty()) {
            return GirlPeekOutcome.TooDark
        }

        return when (Random.nextInt(100)) {
            in 0..4 -> GirlPeekOutcome.CaughtByWerewolves
            in 5..24 -> GirlPeekOutcome.FoundWerewolf(aliveWerewolves.random().uuid)
            else -> GirlPeekOutcome.TooDark
        }
    }

    fun resolveCaughtGirls(actions: List<NightAction>): List<UUID> {
        return actions
            .filterIsInstance<NightAction.GirlPeek>()
            .filter { it.outcome == GirlPeekOutcome.CaughtByWerewolves }
            .map { it.actor }
    }
}
