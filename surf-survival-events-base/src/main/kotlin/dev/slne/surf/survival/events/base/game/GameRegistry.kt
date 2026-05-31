package dev.slne.surf.survival.events.base.game

import dev.slne.surf.survival.events.base.util.Games

/**
 * Registry for game handlers. Event plugins register their [GameHandler]
 * implementations here so the base plugin can delegate game logic without
 * directly depending on event plugins.
 */
object GameRegistry {
    private val handlers = mutableMapOf<Games, GameHandler>()

    fun register(game: Games, handler: GameHandler) {
        handlers[game] = handler
    }

    fun unregister(game: Games) {
        handlers.remove(game)
    }

    fun getHandler(game: Games): GameHandler? = handlers[game]
}
