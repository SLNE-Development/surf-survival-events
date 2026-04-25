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

    private const val UNLIMITED = Int.MAX_VALUE

    private var activeGame: Games? = null
    private var maxPlayers = UNLIMITED

    private val gameQueue = ArrayDeque<UUID>()
    private val waitingQueue = ArrayDeque<UUID>()

    private var task: ScheduledTask? = null

    fun startGame(game: Games, maxPlayers: Int? = null): Boolean {
        if (activeGame != null) return false

        activeGame = game
        this.maxPlayers = maxPlayers ?: UNLIMITED

        if (task == null) {
            task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, { _ ->
                tick()
            }, 0, 1, TimeUnit.SECONDS)
        }

        return true
    }

    fun stopGame(): Boolean {
        if (activeGame == null) return false

        task?.cancel()
        task = null

        activeGame = null
        gameQueue.clear()
        waitingQueue.clear()

        return true
    }

    fun isGameActive(): Boolean = activeGame != null

    fun getActiveGame(): Games =
        activeGame ?: throw IllegalStateException("No active game found")

    fun joinGameQueue(player: Player): Boolean {
        val uuid = player.uniqueId
        if (uuid in gameQueue) return false

        gameQueue.add(uuid)

        player.sendText {
            appendSuccessPrefix()
            success("Du bist jetzt in der Game Lobby!")
        }

        return true
    }

    fun leaveGameQueue(player: Player): Boolean =
        gameQueue.remove(player.uniqueId)

    fun joinWaitingQueue(player: Player): Boolean {
        val uuid = player.uniqueId
        if (uuid in waitingQueue || uuid in gameQueue) return false

        waitingQueue.add(uuid)
        return true
    }

    fun leaveWaitingQueue(player: Player): Boolean =
        waitingQueue.remove(player.uniqueId)

    fun isInGameQueue(player: Player) = player.uniqueId in gameQueue
    fun isInWaitingQueue(player: Player) = player.uniqueId in waitingQueue

    fun getQueuePlayers(): Int = gameQueue.size

    fun getMaxPlayers(): Int =
        if (maxPlayers == UNLIMITED) UNLIMITED else maxPlayers

    fun isQueueFull(): Boolean =
        gameQueue.size >= maxPlayers

    fun setMaxPlayers(maxPlayers: Int) {
        this.maxPlayers = maxPlayers

        while (gameQueue.size > getMaxPlayers()) {
            val uuid = gameQueue.removeLast()

            Bukkit.getPlayer(uuid)?.sendText {
                appendInfoPrefix()
                info("Du wurdest aus der Warteschlange entfernt, da das Limit erreicht wurde.")
            }
        }
    }

    fun checkQueue() {
        if (isQueueFull()) return

        val uuid = waitingQueue.removeFirstOrNull() ?: return
        val player = Bukkit.getPlayer(uuid) ?: return

        joinGameQueue(player)
    }

    private fun tick() {
        checkQueue()

        gameQueue.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let(::showPlayerGameQueue)
        }

        waitingQueue.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let(::showPlayerWaitingQueue)
        }
    }

    fun showPlayerGameQueue(player: Player) {
        val max = if (maxPlayers == UNLIMITED) "unbegrenzt" else getMaxPlayers().toString()

        player.sendActionBar {
            text("Game Lobby: ${gameQueue.size}/$max", TextColor.color(0x6EA6D9))
        }
    }

    fun showPlayerWaitingQueue(player: Player) {
        val position = waitingQueue.indexOf(player.uniqueId) + 1

        player.sendActionBar {
            text(
                "Dein Platz in der Warteschlange: $position/${waitingQueue.size}",
                TextColor.color(0x6EA6D9)
            )
        }
    }
}