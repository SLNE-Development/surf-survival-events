package dev.slne.surf.survival.events.race.region

import org.bukkit.Location


val RegionData.minX get() = minOf(x1, x2)
val RegionData.maxX get() = maxOf(x1, x2)

val RegionData.minY get() = minOf(y1, y2)
val RegionData.maxY get() = maxOf(y1, y2)

val RegionData.minZ get() = minOf(z1, z2)
val RegionData.maxZ get() = maxOf(z1, z2)

fun RegionData.contains(location: Location): Boolean {
    if (location.world?.name != world) return false

    return location.x in minX..maxX &&
            location.y in minY..maxY &&
            location.z in minZ..maxZ
}
