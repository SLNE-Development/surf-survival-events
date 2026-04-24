package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.util.Games
import dev.slne.surf.survival.events.base.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayDeque


object GameService {

    private var activeGame: Games? = null
    private val gameQueue = ArrayDeque<UUID>()
    private val waitingQueue = ArrayDeque<UUID>()

    private var maxPlayers = 1000

    private var task: ScheduledTask? = null
    private var status = false

    fun joinGameQueue(player: Player): Boolean {
        val uuid = player.uniqueId

        if (isGameQueue(player)) return false

        player.sendText {
            appendSuccessPrefix()
            success("Du bist jetzt in der Warteschlange!")
        }
        gameQueue.add(uuid)
        return true
    }

    fun leaveGameQueue(player: Player): Boolean {
        return gameQueue.remove(player.uniqueId)
    }

    fun joinWaitingQueue(player: Player): Boolean {
        val uuid = player.uniqueId

        if (isWaitingQueue(player)) return false
        if (isGameQueue(player)) return false

        waitingQueue.add(uuid)
        return true
    }

    fun leaveWaitingQueue(player: Player): Boolean {
        return waitingQueue.remove(player.uniqueId)
    }

    fun startGame(game: Games, maxPlayers: Int? = null): Boolean {
        if (activeGame != null) return false

        if (!status) {
            status = true

            task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                checkQueue()
            }, 0, 1, TimeUnit.SECONDS)
        }

        activeGame = game
        this.maxPlayers = maxPlayers ?: 1000
        return true
    }

    fun stopGame(): Boolean {
        if (activeGame == null) return false

        if (status) {
            status = false

            task?.cancel()
            task = null
        }
        activeGame = null
        return true
    }

    fun isGameActive(): Boolean {
        return activeGame != null
    }

    fun isGameQueue(player: Player): Boolean {
        val uuid = player.uniqueId
        return gameQueue.contains(uuid)
    }

    fun isWaitingQueue(player: Player): Boolean {
        val uuid = player.uniqueId
        return gameQueue.contains(uuid)
    }

    fun getActiveGame(): Games {
        return activeGame ?: throw IllegalStateException("No active game found")
    }

    fun getMaxPlayers(): Int {
        return maxPlayers
    }

    fun getQueuePlayers(): Int {
        return gameQueue.size
    }

    fun setMaxPlayers(maxPlayers: Int) {
        this.maxPlayers = maxPlayers

        while (gameQueue.size + 1 > maxPlayers) {
            val player = Bukkit.getPlayer(gameQueue.removeLast())
            player?.sendText {
                appendInfoPrefix()
                info("Du wurdest aus der Warteschlange entfernt, da das Limit erreicht wurde.")
            }
        }
    }

    fun checkQueue() {
        if (gameQueue.size < maxPlayers - 1) {
            val uuid = waitingQueue.removeFirstOrNull() ?: return
            val player = Bukkit.getPlayer(uuid) ?: return


            joinGameQueue(player)
        }
    }

}