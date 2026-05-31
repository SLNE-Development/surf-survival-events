package dev.slne.surf.survival.events.example

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ExampleConfig(
    var eventWorld: String = "event_example",
    var gameplay: GameplayConfig = GameplayConfig(),
//    var playerSpawn: EventLocationConfig = EventLocationConfig(world = "event_example"),
//    var spectatorSpawn: EventLocationConfig = EventLocationConfig(world = "event_example"),
//    var reserveSpawn: EventLocationConfig = EventLocationConfig(world = "event_example")
) {
    companion object : SpongeYmlConfigClass<ExampleConfig>(
        ExampleConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class GameplayConfig(
        var minPlayersToStart: Int = 1,

        /** Null means every selected online player starts as active player. */
        var activePlayerLimit: Int? = null,

        /** SPECTATOR, RESERVE or IGNORE for players beyond activePlayerLimit. */
        var overflowPolicy: String = "SPECTATOR",

        /** DENY, SPECTATOR, PLAYER, RESERVE or CUSTOM for joins after the event started. */
        var runningJoinPolicy: String = "SPECTATOR",

        /** Useful on a dedicated event server: new joins can be auto-added while the event runs. */
        var autoJoinRunningPlayers: Boolean = true
    )
}
