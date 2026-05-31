package dev.slne.surf.survival.events.base.game

import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry that connects event modules with the base plugin.
 *
 * Each event plugin should register exactly one [GameHandler] for its [GameKey] during enable and
 * unregister it during disable. The base plugin uses this registry for menus, command arguments and
 * `/survivalevents start <game>`.
 *
 * Example:
 * ```kotlin
 * override suspend fun onEnableAsync() {
 *     GameRegistry.register(MyGame.KEY, MyGame())
 * }
 *
 * override suspend fun onDisableAsync() {
 *     GameRegistry.unregister(MyGame.KEY)
 * }
 * ```
 */
public object GameRegistry {
    private val games = ConcurrentHashMap<GameKey<*>, GameHandler>()

    /**
     * Registers [handler] for [key].
     *
     * Registration fails if another handler already uses the same key. This protects the base command
     * and menus from ambiguous event names.
     */
    public fun <HANDLER : GameHandler> register(key: GameKey<HANDLER>, handler: HANDLER) {
        val previousHandler = games.putIfAbsent(key, handler)

        require(previousHandler == null) {
            "Handler for game ${key.displayName} is already registered"
        }
    }

    /** Removes the handler for [key] if it is currently registered. */
    public fun unregister(key: GameKey<*>) {
        games.remove(key)
    }

    /** Returns `true` if [key] currently has a registered handler. */
    public fun isRegistered(key: GameKey<*>): Boolean {
        return games.containsKey(key)
    }

    /** Returns the typed handler for [key], or `null` if it is not registered. */
    @Suppress("UNCHECKED_CAST")
    public fun <HANDLER : GameHandler> getHandler(key: GameKey<HANDLER>): HANDLER? {
        return games[key] as? HANDLER
    }

    /** Returns the raw handler for dynamic base code that does not know the handler type. */
    public fun getRawHandler(key: GameKey<*>): GameHandler? {
        return games[key]
    }

    /** Returns the raw handler for [key] or throws an error with a readable message. */
    public fun requireRawHandler(key: GameKey<*>): GameHandler {
        return getRawHandler(key) ?: error("No handler registered for game: ${key.displayName}")
    }

    /** Reads the current [GameOptions] from a registered handler. */
    public fun getOptions(key: GameKey<*>): GameOptions? {
        return getRawHandler(key)?.options
    }

    /** Returns all registered keys sorted by display name for stable menus and completions. */
    public fun getRegisteredGameKeys(): List<GameKey<*>> {
        return games.keys.sortedBy { it.displayName.lowercase() }
    }

    /** Finds a registered key by its command key, ignoring case. */
    public fun getRegisteredGameKey(key: String): GameKey<*>? {
        return games.keys.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }
    }
}
