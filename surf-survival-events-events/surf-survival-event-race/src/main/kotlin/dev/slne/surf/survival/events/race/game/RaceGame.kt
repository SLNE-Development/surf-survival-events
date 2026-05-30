package dev.slne.surf.survival.events.race.game

import dev.slne.surf.survival.events.base.game.GameHandler
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import java.util.*

class RaceGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<RaceGame>()
            .key("race")
            .displayName("RACE")
            .skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTEyNDNmN2U3MzhhMTI4MTc2YzUxNzUwMjY1MjRmMGU3NjhkZGU1MzBkZWRjMDU0Nzk3NDJjY2JiZTg5N2U1OCJ9fX0")
            .build()
    }

    override suspend fun beginGame(players: List<UUID>, spectators: Set<UUID>) {
        RaceService.setRaceState(RaceState.LOBBY)

        spectators.forEach(RaceService::addSpectators)
        players.forEach(RaceService::addPlayer)
    }
}
