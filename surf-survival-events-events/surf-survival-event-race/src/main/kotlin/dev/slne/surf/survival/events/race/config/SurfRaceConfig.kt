package dev.slne.surf.survival.events.race.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.survival.events.race.plugin
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SurfRaceConfig(

    var lobbyWorld: String = "world",
    var lobbyX: Double = 0.5,
    var lobbyY: Double = 73.0,
    var lobbyZ: Double = 0.5,
    var lobbyYaw: Float = 0f,
    var lobbyPitch: Float = 0f,


    var startWorld: String = "world",
    var startX: Double = 0.5,
    var startY: Double = 73.0,
    var startZ: Double = 0.5,
    var startYaw: Float = 0f,
    var startPitch: Float = 0f,

    var checkPoints: MutableList<Checkpoint> = mutableListOf(),

    ) {
    companion object : SpongeYmlConfigClass<SurfRaceConfig>(
        SurfRaceConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class Checkpoint(
        var int: Int = 0,

        var world: String = "world",

        var x1: Double = 0.0,
        var y1: Double = 0.0,
        var z1: Double = 0.0,

        var x2: Double = 0.0,
        var y2: Double = 0.0,
        var z2: Double = 0.0

    )
}
