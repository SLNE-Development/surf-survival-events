package dev.slne.surf.survival.events.base.game

import java.util.UUID

/**
 * Interface that event plugins implement to handle game-specific logic.
 * Event plugins register their handler with [GameRegistry] on enable.
 */
interface GameHandler {
    /**
     * Called when the game should begin with the given players and spectators.
     */
    fun beginGame(players: ArrayDeque<UUID>, spectators: Set<UUID>)
}
