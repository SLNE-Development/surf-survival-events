package dev.slne.surf.survival.events.race.service

import java.util.UUID

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

    fun getCheckpoint(uuid: UUID): Int {
        return playerProgress[uuid]?.currentCheckpoint ?: 0
    }

    fun checkpointUp(uuid: UUID) {
        playerProgress[uuid]?.let {
            it.currentCheckpoint++
        }
    }

    fun checkpointReset(uuid: UUID) {
        playerProgress[uuid]?.currentCheckpoint = 0
    }

    fun lapUp(uuid: UUID) {
        playerProgress[uuid]?.let {
            it.currentLap++
        }
    }

    fun getLap(uuid: UUID): Int {
        return playerProgress[uuid]?.currentLap ?: 0
    }

    fun setFinished(uuid: UUID, finished: Boolean) {
        playerProgress[uuid]?.finished = finished
    }

    fun isFinished(uuid: UUID): Boolean {
        return playerProgress[uuid]?.finished ?: false
    }

    fun addPlace(uuid: UUID) {
        if (!places.contains(uuid)) {
            places.addLast(uuid)
        }
    }

    fun getPlace(uuid: UUID): Int {
        return places.indexOf(uuid) + 1
    }

    fun getPlaceList(): ArrayDeque<UUID> {
        return places
    }

    fun removePlayer(uuid: UUID) {
        playerProgress.remove(uuid)
        places.remove(uuid)
    }

    fun clear() {
        playerProgress.clear()
        places.clear()
    }


}


