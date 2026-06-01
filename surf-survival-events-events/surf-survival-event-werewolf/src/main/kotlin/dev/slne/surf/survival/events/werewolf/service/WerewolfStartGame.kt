package dev.slne.surf.survival.events.werewolf.service

import java.util.UUID

object WerewolfStartGame {

    fun startWerewolfGame(
        playerList: Collection<UUID>,
        leaderUuid: UUID? = null,
    ): WerewolfService {
        val gameId = "base-werewolf-${UUID.randomUUID().toString().take(8)}"
        val game = WerewolfGameManager.createGame(gameId, leaderUuid)
            ?: error("Could not create werewolf game with id '$gameId'")

        try {
            playerList
                .distinct()
                .forEach { playerId ->
                    when (val joinResult = game.join(playerId)) {
                        WerewolfJoinResult.Success -> WerewolfGameManager.joinGame(gameId, playerId)
                        WerewolfJoinResult.AlreadyInGame -> Unit
                        WerewolfJoinResult.AlreadyStarted -> error("Werewolf game '$gameId' already started before all players could join")
                        is WerewolfJoinResult.Error -> error("Could not join player '$playerId' to werewolf game '$gameId': ${joinResult.message}")
                    }
                }

            when (val startResult = game.start()) {
                WerewolfStartResult.Success -> return game
                WerewolfStartResult.NotInLobbyPhase -> error("Werewolf game '$gameId' is not in lobby phase")
                is WerewolfStartResult.NotEnoughPlayers -> error("Not enough players for werewolf game '$gameId': ${startResult.current}/${startResult.required}")
                is WerewolfStartResult.Error -> error("Could not start werewolf game '$gameId': ${startResult.message}")
            }
        } catch (exception: Exception) {
            WerewolfGameManager.removeGame(gameId, game.allParticipants)
            throw exception
        }
    }
}
