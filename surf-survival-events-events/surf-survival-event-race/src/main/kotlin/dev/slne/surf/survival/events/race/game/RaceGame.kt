package dev.slne.surf.survival.events.race.game

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.event.unregister
import dev.slne.surf.survival.events.base.game.*
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.listener.RaceListener
import dev.slne.surf.survival.events.race.service.RaceService
import org.bukkit.entity.Player
import java.util.*

class RaceGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<RaceGame>()
            .key("race")
            .displayName("RACE")
            .skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTEyNDNmN2U3MzhhMTI4MTc2YzUxNzUwMjY1MjRmMGU3NjhkZGU1MzBkZWRjMDU0Nzk3NDJjY2JiZTg5N2U1OCJ9fX0")
            .build()
    }

    override val options: GameOptions
        get() {
            val gameplay = RaceConfig.getConfig().gameplay

            return GameOptions(
                minPlayersToStart = 1,
                mode = GameMode.BATCHED,
                start = GameStartOptions(
                    activePlayerLimit = gameplay.playersPerRound,
                    overflow = StartOverflowPolicy.RESERVE
                ),
                spectatorsEnabled = true,
                runningJoinPolicy = RunningJoinPolicy.CUSTOM,
                autoJoinRunningPlayers = true
            )
        }

    context(context: GameContext)
    override suspend fun onStarting() {
        RaceListener.register()
    }

    context(context: GameContext)
    override suspend fun onStarted() {
        RaceService.startSession()
    }

    context(context: GameContext)
    override suspend fun onRunningJoin(player: Player): RunningJoinResult {
        val uuid = player.uniqueId

        // Reconnecting player — restore their previous bracket position
        RaceService.getReconnectJoinResult(uuid)?.let { return it }

        // New late joiner — only allow participation before the first qualifying round completes
        return if (RaceService.canLateJoin()) {
            RunningJoinResult.JOINED_AS_RESERVE
        } else {
            RunningJoinResult.JOINED_AS_SPECTATOR
        }
    }

    context(context: GameContext)
    override suspend fun onRunningPlayerJoin(player: Player) {
        // Only reconnecting players reach here (getReconnectJoinResult returned JOINED_AS_PLAYER)
        RaceService.onPlayerReconnect(player.uniqueId)
    }

    context(context: GameContext)
    override suspend fun onRunningReserveJoin(player: Player) {
        val uuid = player.uniqueId
        if (RaceService.isDisconnected(uuid)) {
            // Reconnecting qualified/waiting player
            RaceService.onPlayerReconnect(uuid)
        } else {
            // New late joiner admitted during first qualifying round
            RaceService.addReserve(uuid)
        }
    }

    context(context: GameContext)
    override suspend fun onRunningSpectatorJoin(player: Player) {
        val uuid = player.uniqueId
        if (RaceService.isDisconnected(uuid)) {
            // Reconnecting eliminated/spectator player
            RaceService.onPlayerReconnect(uuid)
        } else {
            RaceService.addSpectator(uuid)
        }
    }

    context(context: GameContext)
    override suspend fun onParticipantRemove(
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        if (reason == PlayerRemoveReason.DISCONNECT) {
            // Preserve race state — only clean up the entity, keep bracket position
            RaceService.onPlayerDisconnect(uuid)
            return
        }

        when (role) {
            ParticipantRole.PLAYER,
            ParticipantRole.RESERVE -> RaceService.removeParticipant(uuid, teleportToServerLobby = true)

            ParticipantRole.SPECTATOR -> RaceService.removeSpectator(uuid, teleportToServerLobby = true)
        }
    }

    context(context: GameContext)
    override suspend fun onStop(reason: GameStopReason) {
        RaceListener.unregister()
        RaceService.stopRace(context, notifyPlayers = reason != GameStopReason.PLUGIN_DISABLE)
    }
}
