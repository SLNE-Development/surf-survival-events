package dev.slne.surf.survival.events.werewolf.util

sealed class GameOutcome {
    data object VillagersWin : GameOutcome()
    data object WerewolvesWin : GameOutcome()
    data object LoversWin : GameOutcome()
    data object SerialKillerWin : GameOutcome()
}