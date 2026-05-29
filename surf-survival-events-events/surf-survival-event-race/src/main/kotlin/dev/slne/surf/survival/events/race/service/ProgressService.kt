package dev.slne.surf.survival.events.race.service

import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.collections.mutableMapOf
import kotlin.collections.set

data class PlayerRaceProgress(
    var currentCheckpoint: Int = 0,
    var currentLap: Int = 0,
    var finished: Boolean = false
)

object ProgressService {
    private val playerProgress = mutableMapOf<UUID, PlayerRaceProgress>()
    private val places = ArrayDeque<UUID>()

    fun addPlayer(uuid: UUID) {
        playerProgress[uuid] = PlayerRaceProgress()
    }

    fun removePlayer(uuid: UUID) {
        playerProgress.remove(uuid)
        places.remove(uuid)
    }

    fun clear() {
        playerProgress.clear()
        places.clear()
    }

    fun getCheckpoint(uuid: UUID) =
        playerProgress[uuid]?.currentCheckpoint ?: 0


    fun checkpointUp(uuid: UUID, checkpointId: Int) {
        playerProgress[uuid]?.let {
            it.currentCheckpoint = checkpointId
        }
    }

    fun checkpointReset(uuid: UUID) {
        playerProgress[uuid]?.currentCheckpoint = 0
    }

    fun getLap(uuid: UUID) =
        playerProgress[uuid]?.currentLap ?: 0


    fun lapUp(uuid: UUID) {
        playerProgress[uuid]?.let {
            it.currentLap++
        }
    }

    fun isFinished(uuid: UUID) =
        playerProgress[uuid]?.finished ?: false


    fun setFinished(uuid: UUID, finished: Boolean) {
        playerProgress[uuid]?.finished = finished
    }

    fun resetProgress(uuid: UUID) {
        playerProgress[uuid] = PlayerRaceProgress()
        places.remove(uuid)
    }

    fun addPlace(uuid: UUID) {
        if (!places.contains(uuid)) {
            places.addLast(uuid)
        }
    }

    fun getPlace(uuid: UUID) = places.indexOf(uuid) + 1


    fun getPlaceList(): ArrayDeque<UUID> = places
}