package dev.slne.surf.survival.events.werewolf.game

import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.game.GameHandler
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.base.game.GameMode
import dev.slne.surf.survival.events.base.game.GameOptions
import dev.slne.surf.survival.events.base.game.GameStartOptions
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.game.ParticipantRole
import dev.slne.surf.survival.events.base.game.PlayerRemoveReason
import dev.slne.surf.survival.events.base.game.RunningJoinPolicy
import dev.slne.surf.survival.events.base.game.StartOverflowPolicy
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.werewolf.permissions.PermissionRegistry
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfStartGame
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class WerewolfGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<WerewolfGame>()
            .key("werewolf")
            .displayName("WEREWOLF")
            .skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ==")
            .build()
    }

    override val options: GameOptions = GameOptions(
        minPlayersToStart = 1,
        mode = GameMode.ELIMINATION,
        start = GameStartOptions(
            overflow = StartOverflowPolicy.IGNORE,
        ),
        spectatorsEnabled = false,
        runningJoinPolicy = RunningJoinPolicy.DENY,
        autoJoinRunningPlayers = false,
    )

    private var activeGameId: String? = null

    context(context: GameContext)
    override suspend fun onStarted() {
        val game = WerewolfStartGame.startWerewolfGame(
            playerList = context.participantPlayers,
            leaderUuid = resolveLeaderUuid(context),
        )
        activeGameId = game.gameId
    }

    context(context: GameContext)
    override suspend fun onParticipantRemove(
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason,
    ) {
        val game = activeGameId?.let(WerewolfGameManager::getGame) ?: return
        player ?: return
        game.removePlayer(player)
    }

    context(context: GameContext)
    override suspend fun onStop(reason: GameStopReason) {
        val onlinePlayers = context.onlineEventPlayers
        val gameId = activeGameId
        activeGameId = null

        if (gameId != null) {
            WerewolfGameManager.getGame(gameId)?.stop()
            WerewolfGameManager.removeGame(gameId, onlinePlayers)
        }

        onlinePlayers.forEach { player ->
            GameService.teleportToServerLobby(player)
        }
    }

    private fun resolveLeaderUuid(context: GameContext): UUID? {
        val participantIds = context.allEventPlayers.toHashSet()
        return Bukkit.getOnlinePlayers()
            .firstOrNull { it.hasPermission(PermissionRegistry.COMMAND_WEREWOLF_ADMIN) && it.uniqueId !in participantIds }
            ?.uniqueId
            ?: Bukkit.getOnlinePlayers()
                .firstOrNull { it.hasPermission(PermissionRegistry.COMMAND_WEREWOLF_ADMIN) }
                ?.uniqueId
    }
}
