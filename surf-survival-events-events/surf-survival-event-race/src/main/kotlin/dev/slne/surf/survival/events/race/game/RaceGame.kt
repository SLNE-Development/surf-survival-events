package dev.slne.surf.survival.events.race.game

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
import dev.slne.surf.survival.events.base.game.RunningJoinResult
import dev.slne.surf.survival.events.base.game.StartOverflowPolicy
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import org.bukkit.entity.Player
import java.util.UUID

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
            val config = RaceConfig.getConfig()
            val gameplay = config.gameplay

            return GameOptions(
                eventWorld = config.eventWorld,
                minPlayersToStart = gameplay.minPlayersToStart,
                mode = GameMode.BATCHED,
                start = GameStartOptions(
                    activePlayerLimit = gameplay.playersPerRound,
                    overflow = StartOverflowPolicy.SPECTATOR
                ),
                spectatorsEnabled = true,
                runningJoinPolicy = if (gameplay.lateJoinAsSpectator) {
                    RunningJoinPolicy.SPECTATOR
                } else {
                    RunningJoinPolicy.DENY
                },
                autoJoinRunningPlayers = gameplay.autoJoinServerPlayersAsSpectators
            )
        }

    override suspend fun onStarted(context: GameContext) {
        RaceService.setRaceState(RaceState.LOBBY)

        context.gamePlayers.forEach(RaceService::addPlayer)
        context.reservePlayers.forEach(RaceService::addSpectators)
        context.spectators.forEach(RaceService::addSpectators)
    }

    override suspend fun onRunningJoin(context: GameContext, player: Player): RunningJoinResult {
        return if (RaceConfig.getConfig().gameplay.lateJoinAsSpectator) {
            RunningJoinResult.JOINED_AS_SPECTATOR
        } else {
            RunningJoinResult.DENIED
        }
    }

    override suspend fun onRunningSpectatorJoin(context: GameContext, player: Player) {
        RaceService.addSpectators(player.uniqueId)
    }

    override suspend fun onParticipantRemove(
        context: GameContext,
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        if (role == ParticipantRole.PLAYER || uuid in RaceService.getRacePlayers()) {
            if (player != null) {
                RaceService.removePlayer(
                    player = player,
                    teleportToServerLobby = reason != PlayerRemoveReason.DISCONNECT
                )
            } else {
                RaceService.removePlayer(uuid)
            }
        } else {
            RaceService.removeSpectator(
                uuid = uuid,
                teleportToServerLobby = reason != PlayerRemoveReason.DISCONNECT
            )
        }
    }

    override suspend fun onStop(context: GameContext, reason: GameStopReason) {
        RaceService.stopRace(notifyPlayers = reason != GameStopReason.PLUGIN_DISABLE)
    }
}
