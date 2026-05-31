package dev.slne.surf.survival.events.example

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.game.RunningJoinPolicy
import dev.slne.surf.survival.events.base.game.StartOverflowPolicy
import org.bukkit.Location
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ExampleConfig(
    var gameplay: GameplayConfig = GameplayConfig(),
    var playerSpawn: Location = Location(server.getWorld("event_example"), 0.0, 0.0, 0.0),
    var spectatorSpawn: Location = Location(server.getWorld("event_example"), 0.0, 0.0, 0.0),
    var reserveSpawn: Location = Location(server.getWorld("event_example"), 0.0, 0.0, 0.0)
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
        var overflowPolicy: StartOverflowPolicy = StartOverflowPolicy.SPECTATOR,

        /** DENY, SPECTATOR, PLAYER, RESERVE or CUSTOM for joins after the event started. */
        var runningJoinPolicy: RunningJoinPolicy = RunningJoinPolicy.SPECTATOR,

        /** Useful on a dedicated event server: new joins can be auto-added while the event runs. */
        var autoJoinRunningPlayers: Boolean = true
    )
}
