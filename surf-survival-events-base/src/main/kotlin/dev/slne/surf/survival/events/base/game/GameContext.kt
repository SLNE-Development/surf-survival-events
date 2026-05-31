package dev.slne.surf.survival.events.base.game

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/** Immutable view of the current event session. */
public data class GameContext(
    val key: GameKey<*>,
    val options: GameOptions,
    val status: GameStatus,

    /**
     * Players that are currently active in the event.
     */
    val gamePlayers: List<UUID>,

    /** Players attached to the event but not active yet, useful for batched/round events. */
    val reservePlayers: List<UUID>,

    /** Spectators attached to the event. */
    val spectators: Set<UUID>
) {
    val participantPlayers: List<UUID>
        get() = gamePlayers + reservePlayers

    val allEventPlayers: List<UUID>
        get() = participantPlayers + spectators

    val playerCount: Int
        get() = participantPlayers.size

    val activePlayerCount: Int
        get() = gamePlayers.size

    val reservePlayerCount: Int
        get() = reservePlayers.size

    val spectatorCount: Int
        get() = spectators.size

    val onlineGamePlayers: List<Player>
        get() = gamePlayers.mapNotNull(Bukkit::getPlayer)

    val onlineReservePlayers: List<Player>
        get() = reservePlayers.mapNotNull(Bukkit::getPlayer)

    val onlineSpectators: List<Player>
        get() = spectators.mapNotNull(Bukkit::getPlayer)

    val onlineParticipants: List<Player>
        get() = participantPlayers.mapNotNull(Bukkit::getPlayer)

    val onlineEventPlayers: List<Player>
        get() = allEventPlayers.distinct().mapNotNull(Bukkit::getPlayer)
}
