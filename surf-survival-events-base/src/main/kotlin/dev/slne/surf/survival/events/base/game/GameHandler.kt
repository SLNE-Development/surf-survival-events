package dev.slne.surf.survival.events.base.game

import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.*

/**
 * Event plugins implement this interface and register it in [GameRegistry].
 *
 * The base plugin owns the generic dedicated-server lifecycle:
 * online server-lobby players -> /survivalevents start <game> -> running joins/quits -> stop/end.
 * Event plugins only implement the game-specific hooks they need.
 */
public interface GameHandler {
    public val options: GameOptions

    /** Called after the service created the session, before it switches to [GameStatus.RUNNING]. */
    @ApiStatus.OverrideOnly
    public suspend fun onStarting(context: GameContext) {}

    /** Called after the service switched the session to [GameStatus.RUNNING]. */
    @ApiStatus.OverrideOnly
    public suspend fun onStarted(context: GameContext) {}

    /**
     * Called when a player joins the event server after the event already started.
     * Return what the base service should register the player as.
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

    /** Called when a player leaves the server or is removed while the event is active. */
    @ApiStatus.OverrideOnly
    public suspend fun onParticipantRemove(
        context: GameContext,
        uuid: UUID,
        player: Player?,
        role: ParticipantRole,
        reason: PlayerRemoveReason
    ) {
    }

    /** Called when the base service stops or ends the current event session. */
    @ApiStatus.OverrideOnly
    public suspend fun onStop(context: GameContext, reason: GameStopReason) {}
}
