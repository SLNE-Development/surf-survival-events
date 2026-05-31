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

    // BoundingBox.contains uses exclusive max (x < maxX), but region coordinates
    // are block positions (integers). To include the full volume of the block at
    // the max position, we add 1.0 to each max coordinate.
    val box = boundingBox
    return location.x >= box.minX && location.x < box.maxX + 1.0
            && location.y >= box.minY && location.y < box.maxY + 1.0
            && location.z >= box.minZ && location.z < box.maxZ + 1.0
}