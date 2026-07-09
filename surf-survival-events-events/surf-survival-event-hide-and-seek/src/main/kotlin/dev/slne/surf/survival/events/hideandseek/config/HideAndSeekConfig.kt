package dev.slne.surf.survival.events.hideandseek.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.core.config.constraints.PositiveNumber
import dev.slne.surf.survival.events.base.util.GamePosition
import dev.slne.surf.survival.events.hideandseek.plugin
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

enum class SeekerSelection { RANDOM, FIXED }

@ConfigSerializable
data class HideAndSeekConfig(
    var gameplay: GameplayConfig = GameplayConfig(),
    var timers: TimerConfig = TimerConfig(),
    var border: BorderConfig = BorderConfig(),

    @param:Comment("Where all players wait during the lobby countdown and where the seekers wait while the hiders hide.")
    var lobbySpawn: GamePosition = GamePosition(),

    @param:Comment("Where the hiders start hiding and the seekers are released from. Also the world border center.")
    var gameSpawn: GamePosition = GamePosition(),

    @param:Comment("Where spectators watch the game from.")
    var spectatorSpawn: GamePosition = GamePosition()
) {
    companion object : SpongeYmlConfigClass<HideAndSeekConfig>(
        HideAndSeekConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class GameplayConfig(
        @PositiveNumber
        var minPlayersToStart: Int = 2,

        @param:Comment("How many seekers are chosen when the preparation phase starts.")
        @PositiveNumber
        var seekerAmount: Int = 1,

        @param:Comment("How seekers are picked: RANDOM chooses them at random, FIXED uses the configured seeker list (filled up randomly if too few are online).")
        var seekerSelection: SeekerSelection = SeekerSelection.RANDOM,

        @param:Comment("UUIDs of the players that become seekers while seekerSelection is FIXED. Managed via /has seeker add|remove.")
        var fixedSeekers: MutableList<String> = mutableListOf(),

        @param:Comment("If true, caught hiders become seekers. Otherwise they become spectators.")
        var hidersBecomeSeekers: Boolean = false,

        @param:Comment("If true, a single hit by a seeker eliminates a hider.")
        var oneHitKnockOut: Boolean = false,

        @param:Comment("Scale applied to hiders while the game runs.")
        var hiderScale: Double = 1.0,

        @param:Comment("Cooldown in seconds shared by the seeker special items.")
        var specialItemCooldownSeconds: Long = 180,

        @param:Comment("Duration in seconds of the glow effect applied to hiders by the glow item.")
        var glowEffectDurationSeconds: Long = 30
    )

    @ConfigSerializable
    data class TimerConfig(
        @param:Comment("Lobby countdown before roles are assigned.")
        var lobbySeconds: Long = 60,

        @param:Comment("Time the hiders have to hide before the seekers are released.")
        var preparationSeconds: Long = 60,

        @param:Comment("Time the seekers have to find the hiders.")
        var seekSeconds: Long = 600,

        @param:Comment("Celebration time after the winner announcement before players return to the server lobby.")
        var celebrationSeconds: Long = 30
    )

    @ConfigSerializable
    data class BorderConfig(
        @param:Comment("World border radius when the game starts.")
        var startRadius: Int = 1000,

        @param:Comment("World border radius the border shrinks to during the seeking phase.")
        var endRadius: Int = 25,

        @param:Comment("Damage per second outside the world border.")
        var damage: Double = 1.0,

        @param:Comment("Distance outside the border before players start taking damage.")
        var buffer: Double = 0.0
    )
}
