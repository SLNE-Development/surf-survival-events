package dev.slne.surf.survival.events.red.light.green.light.command.util

import org.bukkit.Location
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AreaSelection {
    private val activeTargets = ConcurrentHashMap<UUID, AreaTarget>()
    private val pendingPos1 = ConcurrentHashMap<UUID, Location>()

    fun getActiveTarget(uuid: UUID): AreaTarget? = activeTargets[uuid]

    fun toggle(uuid: UUID, target: AreaTarget): AreaTarget? {
        pendingPos1.remove(uuid)

        val current = activeTargets[uuid]
        if (current == target) {
            activeTargets.remove(uuid)
            return null
        }

        activeTargets[uuid] = target
        return target
    }

    fun disable(uuid: UUID) {
        activeTargets.remove(uuid)
        pendingPos1.remove(uuid)
    }

    fun offerBlock(uuid: UUID, location: Location): Pair<Location, Location>? {
        val pos1 = pendingPos1.putIfAbsent(uuid, location)
        if (pos1 == null) return null

        pendingPos1.remove(uuid)
        return pos1 to location
    }
}