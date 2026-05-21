package dev.slne.surf.survival.events.race.service

import java.util.UUID

data class PlayerRaceProgress(
    var currentCheckpoint: Int = 0,
    var currentLap: Int = 1,
    var finished: Boolean = false
)

object ProgressService {

    private val playerProgress = mutableMapOf<UUID, PlayerRaceProgress>()


    fun addPlayer(uuid: UUID) {
        playerProgress[uuid] = PlayerRaceProgress()
    }

    fun getProgress(uuid: UUID): PlayerRaceProgress? {
        return playerProgress[uuid]
    }

    fun getCheckpoint(uuid: UUID): Int {
        return playerProgress[uuid]?.currentCheckpoint ?: 0
    }

    fun checkpointUp(uuid: UUID) {
        playerProgress[uuid]?.currentCheckpoint =
            playerProgress[uuid]?.currentCheckpoint?.plus(1) ?: 0
    }

    fun lapUp(uuid: UUID, lap: Int) {
        playerProgress[uuid]?.currentLap = lap
    }

    fun getLap(uuid: UUID): Int {
        return playerProgress[uuid]?.currentLap ?: 1
    }

    fun setFinished(uuid: UUID, finished: Boolean) {
        playerProgress[uuid]?.finished = finished
    }

    fun isFinished(uuid: UUID): Boolean {
        return playerProgress[uuid]?.finished ?: false
    }

    fun removePlayer(uuid: UUID) {
        playerProgress.remove(uuid)
    }

    fun clear() {
        playerProgress.clear()
    }


}

