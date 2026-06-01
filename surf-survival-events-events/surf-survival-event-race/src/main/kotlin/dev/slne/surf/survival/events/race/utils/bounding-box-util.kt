package dev.slne.surf.survival.events.race.utils

import org.bukkit.Location
import org.bukkit.util.BoundingBox

fun BoundingBox.containsComplete(location: Location): Boolean {
    return location.x >= minX && location.x < maxX + 1.0
            && location.y >= minY && location.y < maxY + 1.0
            && location.z >= minZ && location.z < maxZ + 1.0
}