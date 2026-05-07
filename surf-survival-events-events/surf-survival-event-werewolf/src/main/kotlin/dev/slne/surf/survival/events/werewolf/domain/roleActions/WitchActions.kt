package dev.slne.surf.event.werewolf.domain.roleActions

import dev.slne.surf.event.werewolf.util.NightAction
import dev.slne.surf.event.werewolf.util.WerewolfPlayer
import java.util.*

object WitchActions {

    fun isValid(
        action: NightAction,
        players: Map<UUID, WerewolfPlayer>,
        werewolfTarget: UUID?,
        dayNumber: Int,
    ): Boolean {
        return when (action) {
            is NightAction.WitchHeal -> {
                val witch = players[action.actor] ?: return false
                witch.hasWitchHealPotion &&
                        players[action.target]?.isAlive == true &&
                        werewolfTarget != null &&
                        action.target == werewolfTarget
            }

            is NightAction.WitchPoison -> {
                val witch = players[action.actor] ?: return false
                witch.hasWitchPoisonPotion &&
                        dayNumber > 1 &&
                        players[action.target]?.isAlive == true &&
                        action.actor != action.target
            }

            else -> false
        }
    }

    fun resolveHealTarget(actions: List<NightAction>): UUID? {
        return actions
            .filterIsInstance<NightAction.WitchHeal>()
            .lastOrNull()
            ?.target
    }

    fun resolvePoisonTarget(actions: List<NightAction>): UUID? {
        return actions
            .filterIsInstance<NightAction.WitchPoison>()
            .lastOrNull()
            ?.target
    }

    fun apply(
        players: Map<UUID, WerewolfPlayer>,
        actions: List<NightAction>,
    ) {
        actions
            .filterIsInstance<NightAction.WitchHeal>()
            .map { it.actor }
            .distinct()
            .forEach { actorId ->
                players[actorId]?.hasWitchHealPotion = false
            }

        actions
            .filterIsInstance<NightAction.WitchPoison>()
            .map { it.actor }
            .distinct()
            .forEach { actorId ->
                players[actorId]?.hasWitchPoisonPotion = false
            }
    }
}
