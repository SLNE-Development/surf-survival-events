package dev.slne.surf.event.werewolf.util

sealed class GameOutcome {
    data object VillagersWin : GameOutcome()
    data object WerewolvesWin : GameOutcome()
    data object LoversWin : GameOutcome()
    data object SerialKillerWin : GameOutcome()
}