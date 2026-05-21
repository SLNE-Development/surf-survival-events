package dev.slne.surf.survival.events.race.region

import org.bukkit.Location


val RegionData.minX get() = minOf(x1, x2)
val RegionData.maxX get() = maxOf(x1, x2)

val RegionData.minY get() = minOf(y1, y2)
val RegionData.maxY get() = maxOf(y1, y2)

val RegionData.minZ get() = minOf(z1, z2)
val RegionData.maxZ get() = maxOf(z1, z2)

fun RegionData.contains(location: Location): Boolean {

    val worldName = location.world?.name ?: return false
    if (worldName != world) return false

    val x = location.blockX
    val y = location.blockY
    val z = location.blockZ

    return x in minX.toInt()..maxX.toInt() &&
            y in minY.toInt()..maxY.toInt() &&
            z in minZ.toInt()..maxZ.toInt()
}
