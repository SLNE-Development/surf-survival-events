package dev.slne.surf.survival.events.base.game

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Immutable snapshot of the current base session.
 *
 * A context is safe to keep for logging and hook-local decisions, but it is not a live mutable view.
 * If a handler needs the latest participants after joins, kicks or internal role changes, call
 * [GameService.snapshot()][dev.slne.surf.survival.events.base.service.GameService.snapshot] again.
 */
public data class GameContext(
    /** Event key that identifies the running handler. */
    val key: GameKey<*>,

    /** Options that were read from the handler when the session started. */
    val options: GameOptions,

    /** Current base lifecycle state. */
    val status: GameStatus,

    /**
     * Default world for this event, loaded or created from [key] before the handler is started.
     *
     * The naming convention is [GameWorldService.worldKeyFor]. Events may ignore this world or load
     * additional worlds themselves, but simple events can use this directly for spawns, arenas and
     * spectator locations.
     */
    val eventWorld: World,

    /** Players that are currently active in the event. */
    val gamePlayers: List<UUID>,

    /** Players attached to the event but not active yet, useful for batched or round events. */
    val reservePlayers: List<UUID>,

    /** Spectators attached to the event. */
    val spectators: Set<UUID>
) {
    /** Name of [eventWorld], useful for configs and messages. */
    val eventWorldName: String
        get() = eventWorld.name

    /** Active players plus reserves, without spectators. */
    val participantPlayers: List<UUID>
        get() = gamePlayers + reservePlayers

    /** Every UUID registered in the session, including spectators. */
    val allEventPlayers: List<UUID>
        get() = participantPlayers + spectators

    /** Number of non-spectator participants. */
    val playerCount: Int
        get() = participantPlayers.size

    /** Number of active players. */
    val activePlayerCount: Int
        get() = gamePlayers.size

    /** Number of reserve players. */
    val reservePlayerCount: Int
        get() = reservePlayers.size

    /** Number of spectators. */
    val spectatorCount: Int
        get() = spectators.size

    /** Online Bukkit players for [gamePlayers]. Offline UUIDs are skipped. */
    val onlineGamePlayers: List<Player>
        get() = gamePlayers.mapNotNull(Bukkit::getPlayer)

    /** Online Bukkit players for [reservePlayers]. Offline UUIDs are skipped. */
    val onlineReservePlayers: List<Player>
        get() = reservePlayers.mapNotNull(Bukkit::getPlayer)

    /** Online Bukkit players for [spectators]. Offline UUIDs are skipped. */
    val onlineSpectators: List<Player>
        get() = spectators.mapNotNull(Bukkit::getPlayer)

    /** Online active and reserve players. Spectators are not included. */
    val onlineParticipants: List<Player>
        get() = participantPlayers.mapNotNull(Bukkit::getPlayer)

    /** Online active players, reserve players and spectators. */
    val onlineEventPlayers: List<Player>
        get() = allEventPlayers.distinct().mapNotNull(Bukkit::getPlayer)

    /** Returns the UUIDs that currently have [role] in this snapshot. */
    public fun playersByRole(role: ParticipantRole): Collection<UUID> {
        return when (role) {
            ParticipantRole.PLAYER -> gamePlayers
            ParticipantRole.RESERVE -> reservePlayers
            ParticipantRole.SPECTATOR -> spectators
        }
    }

    /** Returns the online players that currently have [role] in this snapshot. */
    public fun onlinePlayersByRole(role: ParticipantRole): List<Player> {
        return playersByRole(role).mapNotNull(Bukkit::getPlayer)
    }
}
