package dev.slne.surf.survival.events.werewolf.util

import java.util.UUID

data class VoteStanding(
    val target: UUID,
    val votes: Int
)

data class PhaseAdvanceResult(
    val nextPhase: GameState,
    val winner: GameOutcome? = null,
    val electedMayor: UUID? = null,
    val eliminatedPlayers: List<UUID> = emptyList(),
    val voteStandings: List<VoteStanding> = emptyList()
)
