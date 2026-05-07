package dev.slne.surf.event.werewolf.util

import java.util.*

sealed class GirlPeekOutcome {
    data object TooDark : GirlPeekOutcome()
    data class FoundWerewolf(val target: UUID) : GirlPeekOutcome()
    data object CaughtByWerewolves : GirlPeekOutcome()
}
