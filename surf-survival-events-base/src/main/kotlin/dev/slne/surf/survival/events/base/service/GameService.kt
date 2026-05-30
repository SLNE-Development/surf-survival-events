package dev.slne.surf.survival.events.base.service

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.plugin
import kotlinx.coroutines.Job
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayDeque
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

object GameService {

    private const val UNLIMITED = -1

    private val lock = ReentrantLock()

    private var activeGame: GameKey<*>? = null
    private var maxPlayers = UNLIMITED

    private val lobbyPlayers = linkedSetOf<UUID>()

    private val waitingPlayers = ArrayDeque<UUID>()
    private val waitingPlayerIds = hashSetOf<UUID>()

    private val spectators = hashSetOf<UUID>()

    private var tickJob: Job? = null

    fun startGame(key: GameKey<*>, maxPlayers: Int? = null): Boolean {
        val limit = maxPlayers ?: UNLIMITED
        requireValidLimit(limit)

        return lock.withLock {
            if (activeGame != null) return false

            activeGame = key
            this.maxPlayers = limit

            tickJob = plugin.scope.runAtFixedRate(1.seconds) {
                tick()
            }

            true
        }
    }

    fun stopGame(): GameKey<*>? {
        val stopped = lock.withLock {
            val game = activeGame ?: return null

            val stopped = StoppedGame(
                game = game,
                job = tickJob
            )

            activeGame = null
            maxPlayers = UNLIMITED
            tickJob = null

            lobbyPlayers.clear()
            clearWaitingPlayers()
            spectators.clear()

            stopped
        }

        stopped.job?.cancel()
        return stopped.game
    }

    fun isGameActive(): Boolean {
        return getActiveGameKeyOrNull() != null
    }

    fun getActiveGameKeyOrNull(): GameKey<*>? {
        return lock.withLock {
            activeGame
        }
    }

    fun getActiveGameKey(): GameKey<*> {
        return getActiveGameKeyOrNull() ?: error("No active game found")
    }

    fun onPlayerJoin(player: Player) {
        if (!isGameActive()) return

        AnnouncementService.sendOpenEvent(player)
    }

    fun onPlayerQuit(player: Player) {
        remove(player.uniqueId, includeSpectators = false)
    }

    fun queue(player: Player): QueueJoinResult {
        val uuid = player.uniqueId

        return lock.withLock {
            when {
                activeGame == null -> {
                    QueueJoinResult(QueueJoinType.NO_ACTIVE_GAME)
                }

                uuid in lobbyPlayers || uuid in waitingPlayerIds -> {
                    QueueJoinResult(QueueJoinType.ALREADY_PLAYER)
                }

                uuid in spectators -> {
                    QueueJoinResult(QueueJoinType.ALREADY_SPECTATOR)
                }

                isLobbyFull() -> {
                    addWaitingPlayerLast(uuid)

                    QueueJoinResult(
                        type = QueueJoinType.JOINED_WAITING_LIST,
                        position = waitingPlayers.indexOf(uuid) + 1,
                        size = waitingPlayers.size,
                        maxPlayers = maxPlayersOrNull()
                    )
                }

                else -> {
                    lobbyPlayers.add(uuid)

                    QueueJoinResult(
                        type = QueueJoinType.JOINED_LOBBY,
                        position = lobbyPlayers.size,
                        size = lobbyPlayers.size,
                        maxPlayers = maxPlayersOrNull()
                    )
                }
            }
        }
    }

    fun joinSpectator(player: Player): SpectatorJoinResult {
        val uuid = player.uniqueId

        return lock.withLock {
            when {
                activeGame == null -> SpectatorJoinResult.NO_ACTIVE_GAME
                uuid in lobbyPlayers || uuid in waitingPlayerIds -> SpectatorJoinResult.ALREADY_PLAYER
                uuid in spectators -> SpectatorJoinResult.ALREADY_SPECTATOR
                else -> {
                    spectators.add(uuid)
                    SpectatorJoinResult.JOINED
                }
            }
        }
    }

    fun remove(player: Player, includeSpectators: Boolean = true): RemoveResult {
        return remove(player.uniqueId, includeSpectators)
    }

    fun remove(uuid: UUID, includeSpectators: Boolean = true): RemoveResult {
        val update = lock.withLock {
            when {
                activeGame == null -> {
                    RemoveUpdate(RemoveResult.NO_ACTIVE_GAME)
                }

                lobbyPlayers.remove(uuid) -> {
                    RemoveUpdate(
                        result = RemoveResult.REMOVED_FROM_LOBBY,
                        promoted = promoteWaitingPlayers()
                    )
                }

                removeWaitingPlayer(uuid) -> {
                    RemoveUpdate(RemoveResult.REMOVED_FROM_WAITING_LIST)
                }

                includeSpectators && spectators.remove(uuid) -> {
                    RemoveUpdate(RemoveResult.REMOVED_SPECTATOR)
                }

                else -> {
                    RemoveUpdate(RemoveResult.NOT_PARTICIPATING)
                }
            }
        }

        update.promoted.forEach(::notifyPromoted)
        return update.result
    }

    fun setMaxPlayers(maxPlayers: Int?): Boolean {
        val limit = maxPlayers ?: UNLIMITED
        requireValidLimit(limit)

        val update = lock.withLock {
            if (activeGame == null) return false

            this.maxPlayers = limit

            val demoted = mutableListOf<UUID>()

            while (isLobbyOverLimit()) {
                val uuid = lobbyPlayers.lastOrNull() ?: break

                lobbyPlayers.remove(uuid)
                addWaitingPlayerFirst(uuid)

                demoted += uuid
            }

            LimitUpdate(
                demoted = demoted,
                promoted = promoteWaitingPlayers()
            )
        }

        update.demoted.forEach(::notifyDemoted)
        update.promoted.forEach(::notifyPromoted)

        return true
    }

    suspend fun beginGame(): Boolean {
        val snapshot = lock.withLock {
            val key = activeGame ?: return false

            GameStartSnapshot(
                key = key,
                players = ArrayDeque(lobbyPlayers),
                spectators = spectators.toSet()
            )
        }

        val handler = GameRegistry.getRawHandler(snapshot.key)
            ?: error("No handler registered for game: ${snapshot.key.displayName}")

        handler.beginGame(snapshot.players, snapshot.spectators)
        return true
    }

    fun snapshot(): GameSnapshot? {
        return lock.withLock {
            val key = activeGame ?: return null

            GameSnapshot(
                game = key,
                maxPlayers = maxPlayersOrNull(),
                lobbyPlayers = lobbyPlayers.toList(),
                waitingPlayers = waitingPlayers.toList(),
                spectators = spectators.toSet()
            )
        }
    }

    private fun tick() {
        val snapshot = lock.withLock {
            if (activeGame == null) return

            val promoted = promoteWaitingPlayers()

            TickSnapshot(
                lobbyViewers = (spectators.asSequence() + lobbyPlayers.asSequence())
                    .distinct()
                    .toList(),
                waitingPlayers = waitingPlayers.toList(),
                lobbySize = lobbyPlayers.size,
                maxPlayers = maxPlayersOrNull(),
                promoted = promoted
            )
        }

        snapshot.promoted.forEach(::notifyPromoted)

        snapshot.lobbyViewers.forEach { uuid ->
            showLobbyActionBar(
                uuid = uuid,
                lobbySize = snapshot.lobbySize,
                maxPlayers = snapshot.maxPlayers
            )
        }

        snapshot.waitingPlayers.forEachIndexed { index, uuid ->
            showWaitingActionBar(
                uuid = uuid,
                position = index + 1,
                waitingSize = snapshot.waitingPlayers.size
            )
        }
    }

    private fun promoteWaitingPlayers(): List<Promotion> {
        val promoted = mutableListOf<Promotion>()

        while (!isLobbyFull()) {
            val uuid = pollWaitingPlayer() ?: break

            if (lobbyPlayers.add(uuid)) {
                promoted += Promotion(uuid)
            }
        }

        return promoted
    }

    private fun isLobbyFull(): Boolean {
        return maxPlayers != UNLIMITED && lobbyPlayers.size >= maxPlayers
    }

    private fun isLobbyOverLimit(): Boolean {
        return maxPlayers != UNLIMITED && lobbyPlayers.size > maxPlayers
    }

    private fun maxPlayersOrNull(): Int? {
        return if (maxPlayers == UNLIMITED) null else maxPlayers
    }

    private fun addWaitingPlayerLast(uuid: UUID): Boolean {
        if (!waitingPlayerIds.add(uuid)) return false

        waitingPlayers.addLast(uuid)
        return true
    }

    private fun addWaitingPlayerFirst(uuid: UUID): Boolean {
        if (!waitingPlayerIds.add(uuid)) {
            waitingPlayers.remove(uuid)
        }

        waitingPlayers.addFirst(uuid)
        return true
    }

    private fun removeWaitingPlayer(uuid: UUID): Boolean {
        if (!waitingPlayerIds.remove(uuid)) return false

        waitingPlayers.remove(uuid)
        return true
    }

    private fun pollWaitingPlayer(): UUID? {
        val uuid = waitingPlayers.removeFirstOrNull() ?: return null

        waitingPlayerIds.remove(uuid)
        return uuid
    }

    private fun clearWaitingPlayers() {
        waitingPlayers.clear()
        waitingPlayerIds.clear()
    }

    private fun requireValidLimit(maxPlayers: Int) {
        require(maxPlayers == UNLIMITED || maxPlayers > 0) {
            "maxPlayers must be positive or null"
        }
    }

    private fun notifyPromoted(promotion: Promotion) {
        val player = Bukkit.getPlayer(promotion.uuid) ?: return

        player.sendText {
            appendSuccessPrefix()
            success("Ein Platz ist frei geworden. Du bist jetzt in der Game Lobby!")
        }
    }

    private fun notifyDemoted(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return

        player.sendText {
            appendInfoPrefix()
            info("Du wurdest wegen des neuen Limits in die Warteliste verschoben.")
        }
    }

    private fun showLobbyActionBar(uuid: UUID, lobbySize: Int, maxPlayers: Int?) {
        val player = Bukkit.getPlayer(uuid) ?: return
        val max = maxPlayers?.toString() ?: "unbegrenzt"

        player.sendActionBar {
            text("Game Lobby: $lobbySize/$max", Colors.INFO)
        }
    }

    private fun showWaitingActionBar(uuid: UUID, position: Int, waitingSize: Int) {
        val player = Bukkit.getPlayer(uuid) ?: return

        player.sendActionBar {
            text("Dein Platz in der Warteliste: $position/$waitingSize", Colors.INFO)
        }
    }

    data class QueueJoinResult(
        val type: QueueJoinType,
        val position: Int = -1,
        val size: Int = 0,
        val maxPlayers: Int? = null
    ) {
        val joined: Boolean
            get() = type.joined
    }

    enum class QueueJoinType(
        val joined: Boolean
    ) {
        NO_ACTIVE_GAME(false),
        ALREADY_PLAYER(false),
        ALREADY_SPECTATOR(false),
        JOINED_LOBBY(true),
        JOINED_WAITING_LIST(true)
    }

    enum class SpectatorJoinResult {
        NO_ACTIVE_GAME,
        ALREADY_PLAYER,
        ALREADY_SPECTATOR,
        JOINED
    }

    enum class RemoveResult(
        val removed: Boolean
    ) {
        NO_ACTIVE_GAME(false),
        NOT_PARTICIPATING(false),
        REMOVED_FROM_LOBBY(true),
        REMOVED_FROM_WAITING_LIST(true),
        REMOVED_SPECTATOR(true)
    }

    data class GameSnapshot(
        val game: GameKey<*>,
        val maxPlayers: Int?,
        val lobbyPlayers: List<UUID>,
        val waitingPlayers: List<UUID>,
        val spectators: Set<UUID>
    ) {
        val queuedPlayers: List<UUID>
            get() = lobbyPlayers + waitingPlayers
    }

    private data class RemoveUpdate(
        val result: RemoveResult,
        val promoted: List<Promotion> = emptyList()
    )

    private data class LimitUpdate(
        val demoted: List<UUID>,
        val promoted: List<Promotion>
    )

    private data class Promotion(
        val uuid: UUID
    )

    private data class TickSnapshot(
        val lobbyViewers: List<UUID>,
        val waitingPlayers: List<UUID>,
        val lobbySize: Int,
        val maxPlayers: Int?,
        val promoted: List<Promotion>
    )

    private data class GameStartSnapshot(
        val key: GameKey<*>,
        val players: ArrayDeque<UUID>,
        val spectators: Set<UUID>
    )

    private data class StoppedGame(
        val game: GameKey<*>,
        val job: Job?
    )
}