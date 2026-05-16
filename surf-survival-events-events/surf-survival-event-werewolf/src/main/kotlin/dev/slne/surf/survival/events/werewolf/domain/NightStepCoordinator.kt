package dev.slne.surf.survival.events.werewolf.domain

import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import java.util.*

internal class NightStepCoordinator(
    private val players: Map<UUID, WerewolfPlayer>,
    private val dayNumber: Int,
) {

    fun firstStep(): NightStep {
        return nextAvailableStep()
    }

    fun nextStep(
        currentStep: NightStep?,
        actions: List<NightAction>,
    ): NightStep? {
        val step = currentStep ?: return null
        if (!isStepComplete(step, actions)) return null

        return nextAvailableStep(after = step)
    }

    fun isActionAllowed(
        currentStep: NightStep?,
        action: NightAction,
    ): Boolean {
        return currentStep != null && stepForAction(action) == currentStep
    }

    private fun isStepComplete(
        step: NightStep,
        actions: List<NightAction>,
    ): Boolean {
        if (step == NightStep.RESOLVE) return true

        val actorIds = aliveActorsForStep(step)
        if (actorIds.isEmpty()) return true

        return actorIds.all { actorId ->
            actions.any { action ->
                action.actor == actorId && stepForAction(action) == step
            }
        }
    }

    private fun aliveActorsForStep(step: NightStep): List<UUID> {
        val activeRole = step.activeRole ?: return emptyList()

        return players.values
            .filter { it.isAlive && it.role == activeRole }
            .map { it.uuid }
    }

    private fun nextAvailableStep(after: NightStep? = null): NightStep {
        val steps = NightStep.values()
        val startIndex = after?.let { steps.indexOf(it) + 1 } ?: 0

        return steps
            .drop(startIndex)
            .firstOrNull(::isStepAvailable)
            ?: NightStep.RESOLVE
    }

    private fun isStepAvailable(step: NightStep): Boolean {
        return when (step) {
            NightStep.AMOR -> dayNumber == 1 && hasAliveRole(WerwolfRoles.AMOR)
            NightStep.WEREWOLVES -> hasAliveRole(WerwolfRoles.WERWOLF)
            NightStep.GIRL -> hasAliveRole(WerwolfRoles.GIRL)
            NightStep.SEER -> hasAliveRole(WerwolfRoles.SEER)
            NightStep.DOCTOR -> hasAliveRole(WerwolfRoles.DOCTOR)
            NightStep.WITCH -> hasAliveRole(WerwolfRoles.WITCH)
            NightStep.SERIAL_KILLER -> hasAliveRole(WerwolfRoles.SERIAL_KILLER)
            NightStep.RESOLVE -> true
        }
    }

    private fun hasAliveRole(role: WerwolfRoles): Boolean {
        return players.values.any { it.isAlive && it.role == role }
    }

    private fun stepForAction(action: NightAction): NightStep {
        return when (action) {
            is NightAction.AmorLink -> NightStep.AMOR
            is NightAction.WerewolfKill -> NightStep.WEREWOLVES
            is NightAction.GirlPeek -> NightStep.GIRL
            is NightAction.SeerInspect -> NightStep.SEER
            is NightAction.DoctorProtect -> NightStep.DOCTOR
            is NightAction.WitchHeal,
            is NightAction.WitchPoison -> NightStep.WITCH

            is NightAction.SerialKillerKill -> NightStep.SERIAL_KILLER
        }
    }

    fun nextStepAfterTimeout(currentStep: NightStep?): NightStep? {
        val step = currentStep ?: return null
        return nextAvailableStep(after = step)
    }
}
