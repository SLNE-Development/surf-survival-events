package dev.slne.surf.survival.events.base.game

/**
 * Static metadata and base lifecycle behaviour for one event implementation.
 *
 * The dedicated event server already acts as the pre-game lobby. When an event is started,
 * the base service takes the online players on this server, splits them according to [start],
 * and then hands the resulting [GameContext] to the event handler.
 */
public data class GameOptions(
    /** The world where the actual event takes place. Each event should use its own world. */
    val eventWorld: String = "world", // TODO: Auto create world based on game key and also teleport players to it?

    /** Minimum amount of selected online players required before the event can start. */
    val minPlayersToStart: Int = 1,

    /** Describes how the event itself is structured. Informational for menus and handlers. */
    val mode: GameMode = GameMode.ALL_AT_ONCE,

    /** How the online players should be split when /survivalevents start <game> is executed. */
    val start: GameStartOptions = GameStartOptions(),

    /** Whether players may join as spectators while the event is running. */
    val spectatorsEnabled: Boolean = true,

    /** What should happen when a player joins the event server after the event already started. */
    val runningJoinPolicy: RunningJoinPolicy = RunningJoinPolicy.SPECTATOR,

    /** Whether PlayerJoinEvent should automatically call [GameService.joinRunningEvent(player)][dev.slne.surf.survival.events.base.service.GameService.joinRunningEvent] */
    val autoJoinRunningPlayers: Boolean = true
) {
    init {
        require(minPlayersToStart >= 0) {
            "minPlayersToStart must not be negative"
        }
    }
}

/**
 * Controls which online players become active players at event start and what happens with the rest.
 */
public data class GameStartOptions(
    /** Optional active-player cap at event start, e.g. 10 racers for a race event. */
    val activePlayerLimit: Int? = null,

    /** Behaviour for online players beyond [activePlayerLimit]. */
    val overflow: StartOverflowPolicy = StartOverflowPolicy.SPECTATOR
) {
    init {
        require(activePlayerLimit == null || activePlayerLimit > 0) {
            "activePlayerLimit must be positive or null"
        }
    }
}

public enum class StartOverflowPolicy {
    /** Overflow players are registered as spectators and receive spectator hooks. */
    SPECTATOR,

    /** Overflow players stay attached to the event as reserves for handlers that run batches/rounds. */
    RESERVE,

    /** Overflow players are ignored by the event session. */
    IGNORE
}

public enum class GameMode {
    /** Every selected online player participates at once. */
    ALL_AT_ONCE,

    /** Only a limited active set plays while others watch or wait for the handler. */
    BATCHED,

    /** Round based event where players can be eliminated between rounds. */
    ELIMINATION,

    /** The handler implements custom behaviour. */
    CUSTOM
}

public enum class RunningJoinPolicy {
    /** Players cannot join after the event started. */
    DENY,

    /** Players join as spectators after the event started. */
    SPECTATOR,

    /** Players join as active players after the event started. */
    PLAYER,

    /** Players are attached as reserves after the event started. */
    RESERVE,

    /** The handler decides by overriding GameHandler.onRunningJoin(...). */
    CUSTOM
}

public enum class RunningJoinResult {
    DENIED,
    JOINED_AS_PLAYER,
    JOINED_AS_RESERVE,
    JOINED_AS_SPECTATOR
}

public enum class GameStatus {
    STARTING,
    RUNNING,
    STOPPING
}

public enum class GameStopReason {
    COMMAND,
    HANDLER,
    PLUGIN_DISABLE,
    ERROR
}

public enum class PlayerRemoveReason {
    LEAVE,
    KICK,
    DISCONNECT,
    STOPPED
}

public enum class ParticipantRole {
    PLAYER,
    RESERVE,
    SPECTATOR
}
