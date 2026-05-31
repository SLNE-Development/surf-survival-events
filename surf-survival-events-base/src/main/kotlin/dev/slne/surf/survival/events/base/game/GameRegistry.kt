package dev.slne.surf.survival.events.base.game

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for game handlers. Event plugins register their [GameHandler]
 * implementations here so the base plugin can delegate game logic without
 * directly depending on event plugins.
 */
public object GameRegistry {
    private val games = ConcurrentHashMap<GameKey<*>, GameHandler>()

    public fun <HANDLER : GameHandler> register(key: GameKey<HANDLER>, handler: HANDLER) {
        val previousHandler = games.putIfAbsent(key, handler)

        require(previousHandler == null) {
            "Handler for game ${key.displayName} is already registered"
        }
    }

    public fun unregister(key: GameKey<*>) {
        games.remove(key)
    }

    public fun isRegistered(key: GameKey<*>): Boolean {
        return games.containsKey(key)
    }

    @Suppress("UNCHECKED_CAST")
    public fun <HANDLER : GameHandler> getHandler(key: GameKey<HANDLER>): HANDLER? {
        return games[key] as? HANDLER
    }

    public fun getRawHandler(key: GameKey<*>): GameHandler? {
        return games[key]
    }

    public fun requireRawHandler(key: GameKey<*>): GameHandler {
        return getRawHandler(key) ?: error("No handler registered for game: ${key.displayName}")
    }

    public fun getOptions(key: GameKey<*>): GameOptions? {
        return getRawHandler(key)?.options
    }

    public fun getRegisteredGameKeys(): List<GameKey<*>> {
        return games.keys.sortedBy { it.displayName.lowercase() }
    }

    public fun getRegisteredGameKey(key: String): GameKey<*>? {
        return games.keys.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }
    }
}
