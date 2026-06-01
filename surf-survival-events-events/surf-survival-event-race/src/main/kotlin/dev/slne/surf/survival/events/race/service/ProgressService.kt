package dev.slne.surf.survival.events.race.service

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class PlayerRaceProgress(
    var currentCheckpoint: Int = 0,
    var currentLap: Int = 0,
    var finished: Boolean = false
)

object ProgressService {
    private val lock = ReentrantLock()

    private val playerProgress = Object2ObjectOpenHashMap<UUID, PlayerRaceProgress>()
    private val places = ObjectArrayList<UUID>()

    fun addPlayer(uuid: UUID) {
        lock.withLock {
            playerProgress[uuid] = PlayerRaceProgress()
            places.remove(uuid)
        }
    }

    fun removePlayer(uuid: UUID) {
        lock.withLock {
            playerProgress.remove(uuid)
            places.remove(uuid)
        }
    }

    fun clear() {
        lock.withLock {
            playerProgress.clear()
            places.clear()
        }
    }

    fun getCheckpoint(uuid: UUID): Int = lock.withLock {
        playerProgress[uuid]?.currentCheckpoint ?: 0
    }

    fun checkpointUp(uuid: UUID, checkpointId: Int) {
        lock.withLock {
            playerProgress[uuid]?.currentCheckpoint = checkpointId
        }
    }

    fun checkpointReset(uuid: UUID) {
        lock.withLock {
            playerProgress[uuid]?.currentCheckpoint = 0
        }
    }

    fun getLap(uuid: UUID): Int = lock.withLock {
        playerProgress[uuid]?.currentLap ?: 0
    }

    fun lapUp(uuid: UUID) {
        lock.withLock {
            val progress = playerProgress[uuid] ?: return@withLock
            progress.currentLap++
        }
    }

    fun isFinished(uuid: UUID): Boolean = lock.withLock {
        playerProgress[uuid]?.finished ?: false
    }

    fun setFinished(uuid: UUID, finished: Boolean) {
        lock.withLock {
            playerProgress[uuid]?.finished = finished
        }
    }

    fun resetProgress(uuid: UUID) {
        lock.withLock {
            playerProgress[uuid] = PlayerRaceProgress()
            places.remove(uuid)
        }
    }

    fun addPlace(uuid: UUID) {
        lock.withLock {
            if (uuid !in places) {
                places.add(uuid)
            }
        }
    }

    fun getPlace(uuid: UUID): Int = lock.withLock {
        places.indexOf(uuid) + 1
    }

    fun getPlaceList(): List<UUID> = lock.withLock {
        places.toList()
    }
}
