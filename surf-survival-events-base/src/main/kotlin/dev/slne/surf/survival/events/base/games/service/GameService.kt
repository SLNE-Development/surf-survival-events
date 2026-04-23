package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.survival.events.base.games.util.Games
import org.bukkit.entity.Player
import java.util.UUID



object GameService {
    private val activeGames = mutableListOf<Games>()
    private val gameQueue = mutableListOf<UUID>()

    fun enterPlayerGameQueue(player: Player): Boolean {
        val playerUuid = player.uniqueId
        if (gameQueue.contains(playerUuid)) {
            return false
        }

        gameQueue.add(playerUuid)
        return true
    }

    fun removePlayerGameQueue(player: Player): Boolean {
        val playerUuid = player.uniqueId
        gameQueue.removeIf {
            gameQueue.contains(playerUuid)
            return@removeIf true
        }
        return false
    }


    fun enableGame(game: Games): Boolean {
        if (activeGames.isEmpty()) {
            activeGames.add(game)
            return true
        }
        return false
    }

    fun disableGame(): Boolean {
        activeGames.ifEmpty {
            return false
        }
        activeGames.clear()
        return true
    }

    fun isGameActive(): Boolean {
        return activeGames.isNotEmpty()
    }
}