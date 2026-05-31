package dev.slne.surf.survival.events.race.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.core.config.constraints.PositiveNumber
import dev.slne.surf.survival.events.base.util.GamePosition
import dev.slne.surf.survival.events.race.plugin
import org.bukkit.util.BoundingBox
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class RaceConfig(
    var gameplay: GameplayConfig = GameplayConfig(),

    @param:Comment("Where selected racers are sent in the event world before a round starts.")
    var roundLobbyLocation: GamePosition = GamePosition(0.0, 0.0, 0.0),

    @param:Comment("Where non-racing players should watch from.")
    var spectatorLocation: GamePosition = GamePosition(0.0, 0.0, 0.0),

    var checkpoints: MutableList<CheckPointConfig> = mutableListOf(),
    var barriers: MutableList<BoundingBox> = mutableListOf(),
    var starts: MutableList<StartConfig> = mutableListOf()
) {
    companion object : SpongeYmlConfigClass<RaceConfig>(
        RaceConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class GameplayConfig(
        @param:Comment("Active racers in one heat/final at the same time. Other participants wait as reserves.")
        @PositiveNumber
        var playersPerRound: Int = 10,

        @param:Comment("Default amount of players that advance when /race next-round is used without an argument")
        var qualifiersPerRound: Int = 1,

        @param:Comment("Default amount of winners kept when the final is resolved.")
        var finalWinnerCount: Int = 3,

        @param:Comment("How many laps are there in the race.")
        var laps: Int = 2,

        @param:Comment("Allow players to join the race late, but they will be spectators.")
        var lateJoinAsSpectator: Boolean = true,
    )

    @ConfigSerializable
    data class CheckPointConfig(
        var id: Int,
        val x1: Double, val y1: Double, val z1: Double,
        val x2: Double, val y2: Double, val z2: Double
    ) : BoundingBox(x1, y1, z1, x2, y2, z2)

    data class StartConfig(
        val x1: Double, val y1: Double, val z1: Double,
        val x2: Double, val y2: Double, val z2: Double,
        val yaw: Float, val pitch: Float,
    ) : BoundingBox(x1, y1, z1, x2, y2, z2)
}
