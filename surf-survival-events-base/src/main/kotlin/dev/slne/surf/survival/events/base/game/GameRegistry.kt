package dev.slne.surf.survival.events.base.game

import io.ktor.util.collections.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for game handlers. Event plugins register their [GameHandler]
 * implementations here so the base plugin can delegate game logic without
 * directly depending on event plugins.
 */
object GameRegistry {
    private val games = ConcurrentHashMap<GameKey<*>, GameHandler>()

    fun <HANDLER : GameHandler> register(key: GameKey<HANDLER>, handler: HANDLER) {
        val previousHandler = games.putIfAbsent(key, handler)

        require(previousHandler == null) {
            "Handler for game ${key.displayName} is already registered"
        }
    }

    fun unregister(key: GameKey<*>) {
        games.remove(key)
    }

    @Suppress("UNCHECKED_CAST")
    fun <HANDLER : GameHandler> getHandler(key: GameKey<HANDLER>): HANDLER? {
        return games[key] as? HANDLER
    }

    fun getRawHandler(key: GameKey<*>): GameHandler? {
        return games[key]
    }

    fun getRegisteredGames(): List<GameKey<*>> {
        return games.keys.toList()
    }

    fun getRegisteredGameKey(key: String): GameKey<*>? {
        return games.keys.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }
    }
}
