package dev.slne.surf.survival.events.base.util

import dev.slne.surf.survival.events.base.game.GameContext
import org.bukkit.Location
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
public data class GamePosition(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    @JvmName("toLocation_context")
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

    context(context: GameContext)
    public fun toLocation(): Location {
        return toLocation(context)
    }
}

public fun Location.toGamePosition(): GamePosition {
    return GamePosition(x, y, z, yaw, pitch)
}