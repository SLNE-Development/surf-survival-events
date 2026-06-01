package dev.slne.surf.survival.events.base.game

import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.game.GameWorldService.loadOrCreateEventWorld
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator

/**
 * Resolves the default world for an event from its [GameKey].
 *
 * The convention is intentionally simple: `race` becomes `event_race`, `spleef` becomes
 * `event_spleef`, and so on. This keeps [GameOptions] free of world configuration while still
 * giving every event a predictable, isolated default world.
 *
 * Events that need additional worlds should manage those worlds inside their own module. The base
 * service only guarantees that the default world returned by [loadOrCreateEventWorld] exists before
 * [GameHandler.onStarting] and [GameHandler.onStarted] are called.
 */
public object GameWorldService {
    private const val EVENT_WORLD_NAMESPACE: String = "surf-event"

    private val nonAlphaNumericRegex = Regex("[^a-z0-9_-]")

    /**
     * Returns the default world key for [key] without loading the world.
     *
     * Characters outside `a-z`, `0-9`, `_` and `-` are replaced with `_` so accidental display-like
     * keys cannot create awkward folder names.
     */
    public fun worldKeyFor(key: GameKey<*>): NamespacedKey {
        val sanitized = key.key
            .lowercase()
            .replace(nonAlphaNumericRegex, "_")
            .trim('_', '-')

        return NamespacedKey(EVENT_WORLD_NAMESPACE, sanitized.ifBlank { "unknown" })
    }

    /** Returns the default event world if it is already loaded. */
    public fun getEventWorld(key: GameKey<*>): World? {
        return Bukkit.getWorld(worldKeyFor(key))
    }

    /**
     * Loads or creates the default world for [key].
     *
     * The world is created with Bukkit's normal [WorldCreator] defaults. Event modules that need a
     * custom generator, environment or multiple worlds should not use this helper for those extra
     * worlds; they should create/load them explicitly in their own service.
     *
     * @throws IllegalStateException if Bukkit could not return a loaded world instance.
     */
    public fun loadOrCreateEventWorld(
        key: GameKey<*>,
        creatorCustomization: (WorldCreator) -> Unit,
        worldCustomization: (World) -> Unit
    ): World {
        require(server.isGlobalTickThread) { "Cannot create worlds from non-global tick thread!" }
        val worldKey = worldKeyFor(key)

        val world = WorldCreator(worldKey)
            .apply(creatorCustomization)
            .createWorld()

        if (world == null) {
            throw IllegalStateException("Could not create world $worldKey")
        }

        worldCustomization(world)

        return world
    }
}
