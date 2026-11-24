package dev.slne.surf.survival.events.red.light.green.light.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.core.config.constraints.PositiveNumber
import dev.slne.surf.survival.events.base.util.GamePosition
import dev.slne.surf.survival.events.red.light.green.light.plugin
import org.bukkit.util.BoundingBox
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class RlglConfig(
    var start: BoundingBox = BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    var finish: BoundingBox = BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),

    @param:Comment("Wo Spieler zu Rundenbeginn hin teleportiert werden.")
    var startSpawn: GamePosition = GamePosition(0.0, 0.0, 0.0),

    @param:Comment("Wo eliminierte/fertige Spieler und Zuschauer hin teleportiert werden.")
    var spectatorSpawn: GamePosition = GamePosition(0.0, 0.0, 0.0),

    var gameplay: GameplayConfig = GameplayConfig()
) {
    companion object : SpongeYmlConfigClass<RlglConfig>(
        RlglConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class GameplayConfig(
        @param:Comment("Minimale Dauer einer Grün-Phase in Sekunden.")
        @PositiveNumber
        var minGreenLightSeconds: Int = 3,

        @param:Comment("Maximale Dauer einer Grün-Phase in Sekunden.")
        @PositiveNumber
        var maxGreenLightSeconds: Int = 7,

        @param:Comment("Minimale Dauer einer Rot-Phase in Sekunden.")
        @PositiveNumber
        var minRedLightSeconds: Int = 3,

        @param:Comment("Maximale Dauer einer Rot-Phase in Sekunden.")
        @PositiveNumber
        var maxRedLightSeconds: Int = 6,

        @param:Comment("Reaktionszeit in Millisekunden nach dem Wechsel auf Rot, in der Bewegung noch nicht eliminiert.")
        @PositiveNumber
        var redLightGraceMillis: Long = 400,

        @param:Comment("Umkreis in Blöcken um den Community-Manager, in dem Spieler beim Öffnen einer Runde eingeladen werden.")
        @PositiveNumber
        var inviteRadius: Double = 20.0,

        @param:Comment("Countdown in Sekunden nach /rlgl start, bevor die erste Grün-Phase beginnt.")
        @PositiveNumber
        var startCountdownSeconds: Int = 3
    )
}