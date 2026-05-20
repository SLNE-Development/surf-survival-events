package dev.slne.surf.survival.events.werewolf.service

import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.toBukkitPlayer
import org.bukkit.entity.Player
import java.util.*

object WerewolfGameManager {

    private val games = mutableMapOf<String, WerewolfService>()
    private val playerToGame = mutableMapOf<UUID, String>()

    fun createGame(gameId: String, leaderUuid: UUID): WerewolfService? {
        if (games.containsKey(gameId)) return null
        val game = WerewolfService(gameId)
        game.openLobby(leaderUuid)
        games[gameId] = game
        playerToGame[leaderUuid] = gameId
        WerewolfCommandRequirements.update(leaderUuid.toBukkitPlayer())
        return game
    }

    fun getGame(gameId: String): WerewolfService? {
        return games[gameId]
    }

    fun getGameForPlayer(uuid: UUID): WerewolfService? {
        val gameId = playerToGame[uuid] ?: return null
        return getGame(gameId)
    }

    fun removeGame(gameId: String, participantsToRefresh: Collection<Player> = emptyList()) {
        val playersToUpdate = participantsToRefresh + (games[gameId]?.allParticipants ?: emptyList())
        playerToGame.entries.removeIf { it.value == gameId }
        games.remove(gameId)
        WerewolfCommandRequirements.update(playersToUpdate)
    }

    fun getAllGames(): Map<String, WerewolfService> {
        return games.toMap()
    }

    fun joinGame(gameId: String, uuid: UUID) {
        playerToGame[uuid] = gameId
        WerewolfCommandRequirements.update(uuid.toBukkitPlayer())
    }

    fun handleDisconnect(player: Player) {
        val uuid = player.uniqueId
        val gameId = playerToGame[uuid] ?: return
        val game = games[gameId] ?: run {
            playerToGame.remove(uuid)
            return
        }

        if (game.leader == uuid) {
            val participants = game.allParticipants
            game.stop()
            removeGame(gameId, participants)
            return
        }

        game.removePlayer(player)
        playerToGame.remove(uuid)
    }
}
