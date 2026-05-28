package dev.slne.surf.survival.events.base.service

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.survival.events.base.util.Games
import dev.slne.surf.survival.events.base.plugin
import dev.slne.surf.survival.events.example.ExampleGame.startExampleGame
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayDeque


object GameService {

    private const val NONE = -1

    private var activeGame: Games? = null
    private var maxPlayers = NONE

    private val spectators = mutableListOf<UUID>()

    private val gameQueue = ArrayDeque<UUID>()
    private val waitingQueue = ArrayDeque<UUID>()

    private var task: ScheduledTask? = null

    fun startGame(game: Games, maxPlayers: Int? = null): Boolean {
        if (activeGame != null) return false

        activeGame = game
        this.maxPlayers = maxPlayers ?: NONE

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
        waitingQueue.clear()
        gameQueue.clear()
        spectators.clear()

        return true
    }

    fun isGameActive(): Boolean {
        return activeGame != null
    }

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

    fun getQueuePlayers(): ArrayDeque<UUID> = gameQueue

    fun beginGame() {
        when (activeGame?.displayName) {
            Games.EXAMPLE.displayName -> {
                startExampleGame(getQueuePlayers())
            }

            Games.RACE.displayName -> {
                RaceService.setRaceState(RaceState.LOBBY)
                spectators.forEach {
                    RaceService.addSpectators(it)
                }
                gameQueue.forEach { uuid ->
                    RaceService.addPlayer(uuid)
                }
            }

            else -> {
                throw IllegalStateException("No game found for name: ${activeGame?.displayName}")
            }
        }
    }

    fun getMaxPlayers(): Int =
        if (maxPlayers == NONE) NONE else maxPlayers

    fun isQueueFull(): Boolean =
        gameQueue.size >= maxPlayers

    fun setMaxPlayers(maxPlayers: Int) {
        this.maxPlayers = maxPlayers

        while (gameQueue.size > getMaxPlayers()) {
            val uuid = gameQueue.removeLast()
            val player = Bukkit.getPlayer(uuid) ?: continue
            waitingQueue.addFirst(player.uniqueId)

            player.sendText {
                appendInfoPrefix()
                info("Du wurdest aus der Warteschlange entfernt, da ein neues Limit erreicht wurde.")
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

        spectators.forEach {uuid ->
            Bukkit.getPlayer(uuid)?.let(::showPlayerGameQueue)
        }

        gameQueue.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let(::showPlayerGameQueue)
        }

        waitingQueue.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let(::showPlayerWaitingQueue)
        }
    }

    fun showPlayerGameQueue(player: Player) {
        val max = if (maxPlayers == NONE) "unbegrenzt" else getMaxPlayers().toString()

        player.sendActionBar {
            text("Game Lobby: ${gameQueue.size}/$max", Colors.INFO)
        }
    }

    fun showPlayerWaitingQueue(player: Player) {
        val position = waitingQueue.indexOf(player.uniqueId) + 1

        player.sendActionBar {
            text(
                "Dein Platz in der Warteschlange: $position/${waitingQueue.size}",
                Colors.INFO
            )
        }
    }

    fun addSpectator(uuid: UUID) {
        spectators.add(uuid)
    }

    fun removeSpectator(player: Player): Boolean =
        spectators.remove(player.uniqueId)

    fun isSpectator(uuid: UUID): Boolean = spectators.contains(uuid)
}