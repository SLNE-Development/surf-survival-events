package dev.slne.surf.event.werewolf.service

import java.util.UUID

object WerewolfGameManager {

    private val games = mutableMapOf<String, WerewolfService>()
    private val playerToGame = mutableMapOf<UUID, String>()

    fun createGame(gameId: String, leaderUuid: UUID): WerewolfService? {
        if (games.containsKey(gameId)) return null
        val game = WerewolfService(gameId)
        game.openLobby(leaderUuid)
        games[gameId] = game
        playerToGame[leaderUuid] = gameId
        return game
    }

    fun getGame(gameId: String): WerewolfService? {
        return games[gameId]
    }

    fun getGameForPlayer(uuid: UUID): WerewolfService? {
        val gameId = playerToGame[uuid] ?: return null
        return getGame(gameId)
    }

    fun removeGame(gameId: String) {
        val game = games[gameId] ?: return
        game.players.keys.forEach { playerToGame.remove(it) }
        games.remove(gameId)
    }

    fun getAllGames(): Map<String, WerewolfService> {
        return games.toMap()
    }

    fun joinGame(gameId: String, uuid: UUID) {
        playerToGame[uuid] = gameId
    }
}