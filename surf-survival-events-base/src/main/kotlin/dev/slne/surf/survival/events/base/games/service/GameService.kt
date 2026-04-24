package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.util.Games
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID



object GameService {

    private var activeGame: Games? = null
    private val queue = ArrayDeque<UUID>()

    private var maxPlayers = 1000

    fun joinQueue(player: Player): Boolean {
        val uuid = player.uniqueId

        if (uuid in queue) return false

        queue.add(uuid)
        return true
    }

    fun leaveQueue(player: Player): Boolean {
        return queue.remove(player.uniqueId)
    }

    fun startGame(game: Games, maxPlayers: Int? = null): Boolean {
        if (activeGame != null) return false

        activeGame = game
        this.maxPlayers = maxPlayers ?: 1000
        return true
    }

    fun stopGame(): Boolean {
        if (activeGame == null) return false

        activeGame = null
        return true
    }

    fun isGameActive(): Boolean {
        return activeGame != null
    }

    fun getActiveGame(): Games {
        return activeGame ?: throw IllegalStateException("No active game found")
    }

    fun getMaxPlayers(): Int {
        return maxPlayers
    }

    fun setMaxPlayers(maxPlayers: Int) {
        this.maxPlayers = maxPlayers

        while (queue.size > maxPlayers) {
            val player = Bukkit.getPlayer(queue.removeLast())
            player?.sendText {
                appendInfoPrefix()
                info("Du wurdest aus der Warteschlange entfernt, da das Limit erreicht wurde.")
            }
        }
    }
}