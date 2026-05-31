package dev.slne.surf.survival.events.race.game

import dev.slne.surf.survival.events.base.game.GameHandler
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.collections.Set
import kotlin.collections.forEach

class RaceGameHandler : GameHandler {
    override fun beginGame(players: ArrayDeque<UUID>, spectators: Set<UUID>) {
        RaceService.setRaceState(RaceState.LOBBY)

        spectators.forEach(RaceService::addSpectators)
        players.forEach(RaceService::addPlayer)
    }
}
