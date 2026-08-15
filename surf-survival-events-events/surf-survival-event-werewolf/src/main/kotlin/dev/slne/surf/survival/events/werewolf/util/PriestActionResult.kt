package dev.slne.surf.survival.events.werewolf.util

import java.util.*

sealed class PriestActionResult {
    data class Success(
        val hitWerewolf: Boolean,
        val eliminatedPlayers: List<UUID>,
        val winner: GameOutcome?
    ) : PriestActionResult()

    data object WrongPhase : PriestActionResult()
    data object InvalidActor : PriestActionResult()
    data object InvalidTarget : PriestActionResult()
    data object AlreadyUsed : PriestActionResult()
}
