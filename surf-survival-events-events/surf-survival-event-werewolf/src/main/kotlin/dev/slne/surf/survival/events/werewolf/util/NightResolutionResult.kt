package dev.slne.surf.event.werewolf.util

import java.util.*

data class NightResolutionResult(
    val eliminatedPlayers: List<UUID> = emptyList(),
    val werewolfTarget: UUID? = null,
    val protectedPlayer: UUID? = null,
    val lovers: Pair<UUID, UUID>? = null,
)
