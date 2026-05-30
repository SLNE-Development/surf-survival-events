package dev.slne.surf.survival.events.base.game

import java.util.*

/**
 * Interface that event plugins implement to handle game-specific logic.
 * Event plugins register their handler with [GameRegistry] on enable.
 */
interface GameHandler {

    /**
     * Called when the game should begin with the given players and spectators.
     */
    suspend fun beginGame(players: List<UUID>, spectators: Set<UUID>)
}
