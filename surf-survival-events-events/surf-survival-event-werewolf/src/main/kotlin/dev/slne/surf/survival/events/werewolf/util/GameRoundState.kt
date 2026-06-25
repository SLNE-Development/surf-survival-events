package dev.slne.surf.survival.events.werewolf.util

import java.util.*
import kotlin.time.Duration

data class GameRoundState(
    val phase: GameState,
    var phaseRemainingSeconds: Duration,
    val dayNumber: Int,
    val nightStep: NightStep? = null,
    val nightActions: MutableList<NightAction>,
    val votes: MutableMap<UUID, UUID>,
    val protectedPlayer: UUID? = null,
    val werewolfTarget: UUID? = null,
    val mayorPlayer: UUID? = null,
    val mayorVoteRequired: Boolean = true,
    val mayorVotes: MutableMap<UUID, UUID> = mutableMapOf(),
) {
    companion object {
        fun initial() = GameRoundState(
            phase = GameState.DAY,
            phaseRemainingSeconds = GameState.DAY.time,
            dayNumber = 1,
            nightStep = null,
            nightActions = mutableListOf(),
            votes = mutableMapOf()
        )
    }
}
