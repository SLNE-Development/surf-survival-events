package dev.slne.surf.survival.events.example

import dev.slne.surf.survival.events.base.game.*
import org.bukkit.Bukkit
import org.bukkit.GameMode.ADVENTURE
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Copy this class as a template for new events.
 *
 * Dedicated event-server flow:
 * - players connect to the event server and wait in the server lobby
 * - a manager runs /survivalevents start example
 * - the base service selects the currently online players and calls onStarted(context)
 * - context.gamePlayers contains active players
 * - context.reservePlayers contains overflow players if configured as RESERVE
 * - context.spectators contains overflow players if configured as SPECTATOR plus late spectators
 * - late joins are handled by onRunningJoin(...)
 */
class ExampleGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<ExampleGame>()
            .key("example")
            .displayName("EXAMPLE")
            .skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ")
            .build()
    }

    private val activePlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val reservePlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val activeSpectators = ConcurrentHashMap.newKeySet<UUID>()

    override val options: GameOptions
        get() {
            val config = ExampleConfig.getConfig()
            val gameplay = config.gameplay

            return GameOptions(
                eventWorld = config.eventWorld,
                minPlayersToStart = gameplay.minPlayersToStart,
                mode = if (gameplay.activePlayerLimit == null) GameMode.ALL_AT_ONCE else GameMode.BATCHED,
                start = GameStartOptions(
                    activePlayerLimit = gameplay.activePlayerLimit,
                    overflow = gameplay.overflowPolicy.toStartOverflowPolicy()
                ),
                spectatorsEnabled = true,
                runningJoinPolicy = gameplay.runningJoinPolicy.toRunningJoinPolicy(),
                autoJoinRunningPlayers = gameplay.autoJoinRunningPlayers
            )
        }

    override suspend fun onStarting(context: GameContext) {
        plugin.logger.info(
            "Starting ${context.key.displayName}: " +
                    "players=${context.activePlayerCount}, reserve=${context.reservePlayerCount}, spectators=${context.spectatorCount}"
        )
    }

    override suspend fun onStarted(context: GameContext) {
        context.gamePlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setupPlayer(it) }
        }

        context.reservePlayers.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setupReserve(it) }
        }

        context.spectators.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { setupSpectator(it) }
        }
    }

    override suspend fun onRunningJoin(context: GameContext, player: Player): RunningJoinResult {
        return when (options.runningJoinPolicy) {
            RunningJoinPolicy.DENY -> RunningJoinResult.DENIED
            RunningJoinPolicy.SPECTATOR -> RunningJoinResult.JOINED_AS_SPECTATOR
            RunningJoinPolicy.PLAYER -> RunningJoinResult.JOINED_AS_PLAYER
            RunningJoinPolicy.RESERVE -> RunningJoinResult.JOINED_AS_RESERVE
            RunningJoinPolicy.CUSTOM -> {
                val activeLimit = options.start.activePlayerLimit ?: Int.MAX_VALUE
                if (activePlayers.size < activeLimit) {
                    RunningJoinResult.JOINED_AS_PLAYER
                } else {
                    RunningJoinResult.JOINED_AS_SPECTATOR
                }
            }
        }
    }

    override suspend fun onRunningPlayerJoin(context: GameContext, player: Player) {
        setupPlayer(player)
    }

    override suspend fun onRunningReserveJoin(context: GameContext, player: Player) {
        setupReserve(player)
    }

    override suspend fun onRunningSpectatorJoin(context: GameContext, player: Player) {
        setupSpectator(player)
    }

    override suspend fun onParticipantRemove(
        context: GameContext,
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
        activePlayers.remove(uuid)
        reservePlayers.remove(uuid)
        activeSpectators.remove(uuid)

        player?.inventory?.clear()

        if (player != null && reason != PlayerRemoveReason.DISCONNECT) {
//            player.teleportAsync(SurvivalEventsConfig.getConfig().serverLobby.toLocation()).await()
        }

        plugin.logger.info("${player?.name ?: uuid} left example event as $role because of $reason")
    }

    override suspend fun onStop(context: GameContext, reason: GameStopReason) {
//        val serverLobby = SurvivalEventsConfig.getConfig().serverLobby.toLocation()

        context.onlineEventPlayers.forEach { player ->
            player.inventory.clear()
//            player.teleportAsync(serverLobby).await()
        }

        activePlayers.clear()
        reservePlayers.clear()
        activeSpectators.clear()
        plugin.logger.info("Example event stopped because of $reason")
    }

    private suspend fun setupPlayer(player: Player) {
        activePlayers.add(player.uniqueId)
        reservePlayers.remove(player.uniqueId)
        activeSpectators.remove(player.uniqueId)

        player.gameMode = ADVENTURE
//        player.teleportAsync(ExampleConfig.getConfig().playerSpawn.toLocation()).await()
    }

    private suspend fun setupReserve(player: Player) {
        reservePlayers.add(player.uniqueId)
        activePlayers.remove(player.uniqueId)
        activeSpectators.remove(player.uniqueId)

        player.gameMode = ADVENTURE
//        player.teleportAsync(ExampleConfig.getConfig().reserveSpawn.toLocation()).await()
    }

    private suspend fun setupSpectator(player: Player) {
        activeSpectators.add(player.uniqueId)
        activePlayers.remove(player.uniqueId)
        reservePlayers.remove(player.uniqueId)

//        player.teleportAsync(ExampleConfig.getConfig().spectatorSpawn.toLocation()).await()
    }

    private fun String.toRunningJoinPolicy(): RunningJoinPolicy {
        return runCatching { RunningJoinPolicy.valueOf(uppercase()) }
            .getOrDefault(RunningJoinPolicy.SPECTATOR)
    }

    private fun String.toStartOverflowPolicy(): StartOverflowPolicy {
        return runCatching { StartOverflowPolicy.valueOf(uppercase()) }
            .getOrDefault(StartOverflowPolicy.SPECTATOR)
    }
}
