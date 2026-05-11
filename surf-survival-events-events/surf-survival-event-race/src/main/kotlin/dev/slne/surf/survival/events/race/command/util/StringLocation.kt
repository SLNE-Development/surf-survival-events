package dev.slne.surf.survival.events.race.command.util

import org.bukkit.Location

fun stringLocation(location: Location): String {
    return "${location.world.name} | ${location.x}, ${location.y}, ${location.z}"
}