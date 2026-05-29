package dev.slne.surf.survival.events.race.region

import org.bukkit.Location
import org.bukkit.util.BoundingBox
import java.util.concurrent.ConcurrentHashMap

private val boundingBoxCache = ConcurrentHashMap<RegionData, BoundingBox>()

val RegionData.boundingBox: BoundingBox
    get() = boundingBoxCache.getOrPut(this) {
        BoundingBox(
            minOf(x1, x2),
            minOf(y1, y2),
            minOf(z1, z2),
            maxOf(x1, x2),
            maxOf(y1, y2),
            maxOf(z1, z2)
        )
    }

fun clearBoundingBoxCache() {
    boundingBoxCache.clear()
}

fun RegionData.contains(location: Location): Boolean {
    val world = location.world ?: return false
    if (world.name != this.world) return false

    return boundingBox.contains(
        location.x,
        location.y,
        location.z
    )
}