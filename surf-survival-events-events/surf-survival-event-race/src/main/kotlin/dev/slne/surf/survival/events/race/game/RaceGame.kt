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
                runningJoinPolicy = if (gameplay.lateJoinAsSpectator) {
                    RunningJoinPolicy.SPECTATOR
                } else {
                    RunningJoinPolicy.DENY
                },
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
        return if (RaceConfig.getConfig().gameplay.lateJoinAsSpectator) {
            RunningJoinResult.JOINED_AS_SPECTATOR
        } else {
            RunningJoinResult.DENIED
        }
    }

    context(context: GameContext)
    override suspend fun onRunningReserveJoin(player: Player) {
        RaceService.addReserve(player.uniqueId)
    }

    context(context: GameContext)
    override suspend fun onRunningSpectatorJoin(player: Player) {
        RaceService.addSpectator(player.uniqueId)
    }

    context(context: GameContext)
    override suspend fun onParticipantRemove(
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        val teleportBack = reason != PlayerRemoveReason.DISCONNECT

        when (role) {
            ParticipantRole.PLAYER,
            ParticipantRole.RESERVE -> RaceService.removeParticipant(uuid, teleportBack)

            ParticipantRole.SPECTATOR -> RaceService.removeSpectator(uuid, teleportBack)
        }
    }

    context(context: GameContext)
    override suspend fun onStop(reason: GameStopReason) {
        RaceListener.unregister()
        RaceService.stopRace(notifyPlayers = reason != GameStopReason.PLUGIN_DISABLE)
    }
}
