package dev.slne.surf.survival.events.werewolf.domain

import dev.slne.surf.survival.events.werewolf.domain.roleActions.*
import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.NightResolutionResult
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import dev.slne.surf.survival.events.werewolf.domain.roleActions.AmorActions
import java.util.*

internal class NightResolver(
    private val players: Map<UUID, WerewolfPlayer>,
    private val dayNumber: Int,
    private val werewolfTarget: UUID?,
    private val serialKillerTarget: UUID?,
) {

    fun isValid(action: NightAction, actorRole: WerwolfRoles): Boolean = when (action) {
            is NightAction.WerewolfKill -> actorRole == WerwolfRoles.WERWOLF &&
                    isValidLivingTarget(action.target) &&
                    action.actor != action.target

            is NightAction.GirlPeek -> actorRole == WerwolfRoles.GIRL &&
                    GirlActions.isValid(action, players)

            is NightAction.SeerInspect -> actorRole == WerwolfRoles.SEER &&
                    SeerActions.isValid(action, players)

            is NightAction.DoctorProtect -> actorRole == WerwolfRoles.DOCTOR &&
                    DoctorActions.isValid(action, players)

            is NightAction.WitchHeal,
            is NightAction.WitchPoison -> actorRole == WerwolfRoles.WITCH &&
                    WitchActions.isValid(action, players, werewolfTarget, serialKillerTarget, dayNumber)

            is NightAction.AmorLink -> actorRole == WerwolfRoles.AMOR &&
                    AmorActions.isValid(action, players, dayNumber)

            is NightAction.SerialKillerKill -> actorRole == WerwolfRoles.SERIAL_KILLER &&
                    SerialKillerActions.isValid(action, players)
        }

    fun resolve(actions: List<NightAction>): NightResolutionResult {
        val lovers = AmorActions.resolve(actions)
        val resolvedWerewolfTarget = resolveWerewolfTarget(actions)
        val caughtGirls = GirlActions.resolveCaughtGirls(actions)
        val serialKillerTarget = SerialKillerActions.resolveTarget(actions)
        val doctorProtectedPlayer = DoctorActions.resolveTarget(actions)
        val witchHealTarget = WitchActions.resolveHealTarget(actions)
        val witchPoisonTarget = WitchActions.resolvePoisonTarget(actions)
        val protectedPlayers = setOfNotNull(doctorProtectedPlayer, witchHealTarget)
        val eliminatedPlayers = linkedSetOf<UUID>()

        if (resolvedWerewolfTarget != null &&
            resolvedWerewolfTarget !in protectedPlayers &&
            players[resolvedWerewolfTarget]?.role != WerwolfRoles.SERIAL_KILLER
        ) {
            eliminatedPlayers.add(resolvedWerewolfTarget)
        }

        eliminatedPlayers.addAll(caughtGirls)

        if (serialKillerTarget != null &&
            serialKillerTarget !in protectedPlayers
        ) {
            eliminatedPlayers.add(serialKillerTarget)
        }

        if (witchPoisonTarget != null) {
            eliminatedPlayers.add(witchPoisonTarget)
        }

        return NightResolutionResult(
            eliminatedPlayers = eliminatedPlayers.toList(),
            werewolfTarget = resolvedWerewolfTarget,
            protectedPlayer = witchHealTarget ?: doctorProtectedPlayer,
            lovers = lovers
        )
    }

    private fun isValidLivingTarget(target: UUID): Boolean = players[target]?.isAlive == true

    fun resolveWerewolfTarget(actions: List<NightAction>): UUID? {
        val werewolfActions = actions.filterIsInstance<NightAction.WerewolfKill>()
        if (werewolfActions.isEmpty()) return null

        val counts = werewolfActions.groupingBy { it.target }.eachCount()
        val winner = counts.maxByOrNull { it.value } ?: return null

        return if (winner.value > werewolfActions.size / 2) {
            winner.key
        } else {
            werewolfActions.random().target
        }
    }
}
