package dev.slne.surf.survival.events.base.game

import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/**
 * Contract implemented by event modules and registered in [GameRegistry].
 *
 * The base plugin owns the lifecycle that is identical for every event on the dedicated event
 * server: collect eligible online players, create a session, load the event's default world, track
 * players/reserves/spectators, handle late joins and detach the session on stop.
 *
 * The event plugin owns everything that makes the game unique: teleport positions, items, mobs,
 * scoring, rounds, brackets, parallel arenas and cleanup of its own resources. Most hooks have a
 * default no-op implementation so simple events only override [onStarted] and [onStop].
 */
public interface GameHandler {
    /**
     * Static options read by the base service before a session starts.
     *
     * This property may read the event's config. Avoid returning values that mutate while a session
     * is running; the base service stores the options in [GameContext.options] at session creation.
     */
    public val options: GameOptions

    @ApiStatus.OverrideOnly
    public fun customizeWorldCreator(creator: WorldCreator) {}

    @ApiStatus.OverrideOnly
    public fun customizeEventWorld(world: World) {}

    /**
     * Called after the session and [GameContext.eventWorld] were created, before the status becomes
     * [GameStatus.RUNNING].
     *
     * Use this for validation or one-time world preparation. If this function throws, the base
     * service cancels the startup and calls [onStop] with [GameStopReason.ERROR].
     */
    @ApiStatus.OverrideOnly
    public suspend fun onStarting(context: GameContext) {}

    /**
     * Called after the base service switched the session to [GameStatus.RUNNING].
     *
     * This is the usual place to teleport players, equip them and initialize the event's own state
     * machine. For events without rounds, [GameContext.gamePlayers] normally contains every selected
     * player. For batched events, [GameContext.reservePlayers] contains players that the handler can
     * activate later.
     */
    @ApiStatus.OverrideOnly
    public suspend fun onStarted(context: GameContext) {}

    /**
     * Decides what should happen when [player] joins the event server after the session started.
     *
     * The returned [RunningJoinResult] tells the base service which role should be registered. After
     * registration, the matching hook [onRunningPlayerJoin], [onRunningReserveJoin] or
     * [onRunningSpectatorJoin] is called.
     *
     * The default implementation follows [GameOptions.runningJoinPolicy]. Override this for dynamic
     * logic, for example adding late players to reserves only while qualification rounds are still
     * open.
     */
    @ApiStatus.OverrideOnly
    public suspend fun onRunningJoin(context: GameContext, player: Player): RunningJoinResult {
        return when (options.runningJoinPolicy) {
            RunningJoinPolicy.DENY -> RunningJoinResult.DENIED
            RunningJoinPolicy.SPECTATOR -> if (options.spectatorsEnabled) {
                RunningJoinResult.JOINED_AS_SPECTATOR
            } else {
                RunningJoinResult.DENIED
            }

            RunningJoinPolicy.PLAYER -> RunningJoinResult.JOINED_AS_PLAYER
            RunningJoinPolicy.RESERVE -> RunningJoinResult.JOINED_AS_RESERVE
            RunningJoinPolicy.CUSTOM -> RunningJoinResult.DENIED
        }
    }

    /** Called after the base service registered a late join as active player. */
    @ApiStatus.OverrideOnly
    public suspend fun onRunningPlayerJoin(context: GameContext, player: Player) {}

    /** Called after the base service registered a late join as reserve player. */
    @ApiStatus.OverrideOnly
    public suspend fun onRunningReserveJoin(context: GameContext, player: Player) {}

    /** Called after the base service registered a late join as spectator. */
    @ApiStatus.OverrideOnly
    public suspend fun onRunningSpectatorJoin(context: GameContext, player: Player) {}

    /**
     * Called when [GameService][dev.slne.surf.survival.events.base.service.GameService] changes a
     * participant's role after the session has started.
     *
     * This is mainly useful for event-owned round systems that call `setParticipantRole`, for example
     * moving a reserve into the active group or turning an eliminated player into a spectator. The
     * base service already changed its snapshot before this hook runs.
     */
    @ApiStatus.OverrideOnly
    public suspend fun onParticipantRoleChange(
        context: GameContext,
        uuid: UUID,
        previousRole: ParticipantRole,
        newRole: ParticipantRole
    ) {
    }

    /**
     * Called when a player leaves, disconnects or is removed while the event is active.
     *
     * [player] is `null` when the UUID is currently offline. Use [reason] to avoid teleporting a
     * disconnected player or to show different messages for kicks and voluntary leaves.
     */
    @ApiStatus.OverrideOnly
    public suspend fun onParticipantRemove(
        context: GameContext,
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
    }

    /**
     * Called when the base service stops or ends the current event session.
     *
     * Cancel tasks, despawn event-owned entities, clear score/progress state and usually send online
     * players back to the server lobby with
     * [GameService.teleportToServerLobby][dev.slne.surf.survival.events.base.service.GameService.teleportToServerLobby].
     */
    @ApiStatus.OverrideOnly
    public suspend fun onStop(context: GameContext, reason: GameStopReason) {}
}
