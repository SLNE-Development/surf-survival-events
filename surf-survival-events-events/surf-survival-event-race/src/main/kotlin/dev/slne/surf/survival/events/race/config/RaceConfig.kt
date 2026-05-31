package dev.slne.surf.survival.events.race.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.region.RegionData
import org.bukkit.Location
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class RaceConfig(
    /** The dedicated world where the race takes place. */
    var eventWorld: String = "event_race",

    var gameplay: GameplayConfig = GameplayConfig(),

    /** Where selected racers are sent in the event world before a round starts. */
    var roundLobbyLocation: Location = Location(server.getWorld("event_race"), 0.0, 0.0, 0.0),

    /** Where non-racing players should watch from. */
    var spectatorLocation: Location = Location(server.getWorld("event_race"), 0.0, 0.0, 0.0),

    var checkpoints: MutableList<CheckPointConfig> = mutableListOf(),
    var barriers: MutableList<BarrierConfig> = mutableListOf(),
    var starts: MutableList<StartConfig> = mutableListOf()
) {
    companion object : SpongeYmlConfigClass<RaceConfig>(
        RaceConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class GameplayConfig(
        /** Minimum online players required on the dedicated event server. */
        var minPlayersToStart: Int = 1,

        /** Active racers at event start. Other online players become spectators. */
        var playersPerRound: Int = 10,
        var laps: Int = 5,
        var lateJoinAsSpectator: Boolean = true,
        var autoJoinServerPlayersAsSpectators: Boolean = true
    )

    @ConfigSerializable
    data class CheckPointConfig(
        var id: Int = 0,
        override var world: String = "event_race",
        override var x1: Double = 0.0,
        override var y1: Double = 0.0,
        override var z1: Double = 0.0,
        override var x2: Double = 0.0,
        override var y2: Double = 0.0,
        override var z2: Double = 0.0
    ) : RegionData

    @ConfigSerializable
    data class BarrierConfig(
        override var world: String = "event_race",
        override var x1: Double = 0.0,
        override var y1: Double = 0.0,
        override var z1: Double = 0.0,
        override var x2: Double = 0.0,
        override var y2: Double = 0.0,
        override var z2: Double = 0.0
    ) : RegionData

    @ConfigSerializable
    data class StartConfig(
        override var world: String = "event_race",
        override var x1: Double = 0.0,
        override var y1: Double = 0.0,
        override var z1: Double = 0.0,
        override var x2: Double = 0.0,
        override var y2: Double = 0.0,
        override var z2: Double = 0.0,
        var yaw: Float = 0f,
        var pitch: Float = 0f
    ) : RegionData
}
