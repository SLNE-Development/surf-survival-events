package dev.slne.surf.survival.events.base.games.service

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.survival.events.base.games.util.Games
import dev.slne.surf.survival.events.base.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayDeque


object GameService {

    private var activeGame: Games? = null
    private val gameQueue = ArrayDeque<UUID>()
    private val waitingQueue = ArrayDeque<UUID>()

    private var maxPlayers = 2147483647

    private var task: ScheduledTask? = null
    private var status = false

    fun joinGameQueue(player: Player): Boolean {
        val uuid = player.uniqueId

        if (isGameQueue(player)) return false

        player.sendText {
            appendSuccessPrefix()
            success("Du bist jetzt in der Game Lobby!")
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

                for (uuid in waitingQueue) {
                    val player = Bukkit.getPlayer(uuid) ?: continue
                    showPlayerWaitingQueue(player)
                }

                for (uuid in gameQueue) {
                    val player = Bukkit.getPlayer(uuid) ?: continue
                    showPlayerGameQueue(player)
                }

            }, 0, 1, TimeUnit.SECONDS)
        }

        activeGame = game
        this.maxPlayers = maxPlayers ?: 2147483647
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
        waitingQueue.clear()
        gameQueue.clear()
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
        return waitingQueue.contains(uuid)
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

    fun showPlayerGameQueue(player: Player) {
        val newMaxPlayers = if (maxPlayers == 2147483647) "unbegrenzt" else (maxPlayers - 1).toString()

        player.sendActionBar {
            text("Game Lobby: ${gameQueue.size}/$newMaxPlayers", TextColor.color(0x6EA6D9))
        }
    }

    fun showPlayerWaitingQueue(player: Player) {
        player.sendActionBar {
            text("Dein Platz in der Warteschlange: ${waitingQueue.indexOf(player.uniqueId) + 1}/${waitingQueue.size}", TextColor.color(0x6EA6D9))
        }
    }

}