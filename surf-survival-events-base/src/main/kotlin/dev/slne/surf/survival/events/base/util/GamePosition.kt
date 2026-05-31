package dev.slne.surf.survival.events.base.util

import dev.slne.surf.survival.events.base.game.GameContext
import org.bukkit.Location
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
public data class GamePosition(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    public fun toLocation(context: GameContext): Location {
        return Location(
            context.eventWorld,
            x,
            y,
            z,
            yaw,
            pitch
        )
    }
}

public fun Location.toGamePosition(): GamePosition {
    return GamePosition(x, y, z, yaw, pitch)
}