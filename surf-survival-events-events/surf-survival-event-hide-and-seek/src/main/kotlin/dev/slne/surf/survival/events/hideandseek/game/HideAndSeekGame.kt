package dev.slne.surf.survival.events.hideandseek.game

import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.event.unregister
import dev.slne.surf.survival.events.base.game.*
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.listener.HideAndSeekCombatListener
import dev.slne.surf.survival.events.hideandseek.listener.HideAndSeekRespawnListener
import dev.slne.surf.survival.events.hideandseek.listener.HideAndSeekRestrictionListener
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import org.bukkit.GameRules
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*

class HideAndSeekGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<HideAndSeekGame>()
            .key("hideandseek")
            .displayName("HIDE AND SEEK")
            .skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlmMjAxZjdkMGQ3ZDIyOWQ3YjU1NGRmYTgzMjgzNDE3ZmVjNDgzOGQ1ZjRkMWJlMDJmZTVjMTkwZWJlYjA0ZSJ9fX0=")
            .build()
    }

    override val options: GameOptions
        get() = GameOptions(
            minPlayersToStart = HideAndSeekConfig.getConfig().gameplay.minPlayersToStart,
            mode = GameMode.ELIMINATION,
            spectatorsEnabled = true,
            runningJoinPolicy = RunningJoinPolicy.CUSTOM,
            autoJoinRunningPlayers = true
        )

    override fun customizeEventWorld(world: World) {
        world.setGameRule(GameRules.PVP, true)
        world.setGameRule(GameRules.SPAWN_MOBS, false)
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
        world.setGameRule(GameRules.LOCATOR_BAR, false)
    }

    context(context: GameContext)
    override suspend fun onStarting() {
        HideAndSeekCombatListener.register()
        HideAndSeekRestrictionListener.register()
        HideAndSeekRespawnListener.register()
    }

    context(context: GameContext)
    override suspend fun onStarted() {
        HideAndSeekService.startSession()
    }

    context(context: GameContext)
    override suspend fun onRunningJoin(player: Player): RunningJoinResult {
        return if (HideAndSeekService.phase == HideAndSeekService.Phase.LOBBY) {
            RunningJoinResult.JOINED_AS_PLAYER
        } else {
            RunningJoinResult.JOINED_AS_SPECTATOR
        }
    }

    context(context: GameContext)
    override suspend fun onRunningPlayerJoin(player: Player) {
        HideAndSeekRoleManager.setupLobbyPlayer(player, context)
    }

    context(context: GameContext)
    override suspend fun onRunningSpectatorJoin(player: Player) {
        HideAndSeekRoleManager.setupSpectator(player, context)
    }

    context(context: GameContext)
    override suspend fun onParticipantRemove(
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        HideAndSeekRoleManager.handleParticipantRemove(
            uuid,
            player,
            disconnected = reason == PlayerRemoveReason.DISCONNECT
        )
    }

    context(context: GameContext)
    override suspend fun onStop(reason: GameStopReason) {
        HideAndSeekCombatListener.unregister()
        HideAndSeekRestrictionListener.unregister()
        HideAndSeekRespawnListener.unregister()
        HideAndSeekService.stopSession(reason)
    }
}
