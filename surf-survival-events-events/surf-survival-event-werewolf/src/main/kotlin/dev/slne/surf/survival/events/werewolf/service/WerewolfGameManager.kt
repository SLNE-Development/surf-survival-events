package dev.slne.surf.survival.events.werewolf.service

import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.scoreboard.addToWerewolfScoreboard
import dev.slne.surf.survival.events.werewolf.scoreboard.removeFromWerewolfScoreboard
import dev.slne.surf.survival.events.werewolf.util.toBukkitPlayer
import org.bukkit.entity.Player
import java.util.*


object WerewolfGameManager {

    private val lock = Any()

    private val games = mutableMapOf<String, WerewolfService>()
    private val playerToGame = mutableMapOf<UUID, String>()

    private var baseSessionGameId: String? = null

    fun createGame(gameId: String, leaderUuid: UUID? = null): WerewolfService? {
        val game = synchronized(lock) {
            if (games.containsKey(gameId)) return null
            val game = WerewolfService(gameId)
            game.openLobby(leaderUuid)
            games[gameId] = game
            leaderUuid?.let { playerToGame[it] = gameId }
            game
        }

        leaderUuid?.let {
            it.toBukkitPlayer()?.addToWerewolfScoreboard()
            WerewolfCommandRequirements.update(it.toBukkitPlayer())
        }
        return game
    }

    fun getGame(gameId: String): WerewolfService? = synchronized(lock) { games[gameId] }

    fun getGameForPlayer(uuid: UUID): WerewolfService? = synchronized(lock) {
        playerToGame[uuid]?.let { games[it] }
    }

    fun removeGame(gameId: String, participantsToRefresh: Collection<Player> = emptyList()) {
        val playersToUpdate = synchronized(lock) {
            val game = games.remove(gameId)
            val playersToUpdate = participantsToRefresh + (game?.allParticipants ?: emptyList())
            game?.stop()
            playerToGame.entries.removeIf { it.value == gameId }
            if (baseSessionGameId == gameId) baseSessionGameId = null
            playersToUpdate
        }

        playersToUpdate.distinctBy(Player::getUniqueId).forEach(Player::removeFromWerewolfScoreboard)
        WerewolfCommandRequirements.update(playersToUpdate)
    }

    fun getAllGames(): Map<String, WerewolfService> = synchronized(lock) { games.toMap() }

    fun joinGame(gameId: String, uuid: UUID) {
        synchronized(lock) { playerToGame[uuid] = gameId }
        WerewolfCommandRequirements.update(uuid.toBukkitPlayer())
    }

    fun leaveGame(uuid: UUID) {
        synchronized(lock) { playerToGame.remove(uuid) }
        WerewolfCommandRequirements.update(uuid.toBukkitPlayer())
    }

    fun markAsBaseSession(gameId: String) {
        synchronized(lock) { baseSessionGameId = gameId }
    }

    fun isBaseSession(gameId: String): Boolean = synchronized(lock) { baseSessionGameId == gameId }

    fun handleDisconnect(player: Player) {
        val uuid = player.uniqueId
        val (game, gameId, isLeader) = synchronized(lock) {
            val gameId = playerToGame[uuid] ?: return
            val game = games[gameId] ?: run {
                playerToGame.remove(uuid)
                return
            }
            Triple(game, gameId, game.leader == uuid)
        }

        if (isLeader) {
            if (isBaseSession(gameId)) {
                GameService.stopGame(GameStopReason.HANDLER)
            } else {
                removeGame(gameId, game.allParticipants)
            }
            return
        }

        game.removePlayer(player)
        leaveGame(uuid)
    }
}