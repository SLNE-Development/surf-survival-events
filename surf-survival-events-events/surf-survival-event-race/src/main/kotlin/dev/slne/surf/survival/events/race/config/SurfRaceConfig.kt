package dev.slne.surf.survival.events.race.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.region.RegionData
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SurfRaceConfig(

    var lobbyWorld: String = "world",
    var lobbyX: Double = 0.5,
    var lobbyY: Double = 73.0,
    var lobbyZ: Double = 0.5,
    var lobbyYaw: Float = 0f,
    var lobbyPitch: Float = 0f,

    var checkPoints: MutableList<Checkpoint> = mutableListOf(),
    var barrier: MutableList<Barrier> = mutableListOf(),
    var start: MutableList<Start> = mutableListOf(),

    ) {
    companion object : SpongeYmlConfigClass<SurfRaceConfig>(
        SurfRaceConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class Checkpoint(

        var id: Int = 0,

        override var world: String = "world",

        override var x1: Double = 0.0,
        override var y1: Double = 0.0,
        override var z1: Double = 0.0,

        override var x2: Double = 0.0,
        override var y2: Double = 0.0,
        override var z2: Double = 0.0

    ) : RegionData

    @ConfigSerializable
    data class Barrier(

        override var world: String = "world",

        override var x1: Double = 0.0,
        override var y1: Double = 0.0,
        override var z1: Double = 0.0,

        override var x2: Double = 0.0,
        override var y2: Double = 0.0,
        override var z2: Double = 0.0

    ) : RegionData

    @ConfigSerializable
    data class Start(

        override var world: String = "world",

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
