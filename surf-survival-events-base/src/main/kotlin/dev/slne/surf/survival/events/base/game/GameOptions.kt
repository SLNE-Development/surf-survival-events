package dev.slne.surf.survival.events.base.game

/**
 * Static metadata and generic lifecycle behaviour for one registered event.
 *
 * `GameOptions` intentionally only describes decisions that the base module can handle for every
 * event in the same way: how many players are required, which players are active at the beginning,
 * whether spectators are allowed and how late joins should be treated.
 *
 * The event world is not configurable here. The base module loads or creates one default world from
 * the [GameKey] before the handler is called and exposes it through [GameContext.eventWorld]. Events
 * that need more than one world should create/load those extra worlds themselves inside their own
 * handler/service.
 *
 * Round or bracket logic is also not part of this class. Use [GameStartOptions] to decide the
 * initial active/reserve split and then let the event implementation decide how reserves are turned
 * into active players.
 */
public data class GameOptions(
    /**
     * Minimum number of eligible online players required before `/survivalevents start <game>` can
     * create a session.
     *
     * Eligibility is controlled by the base config, for example by excluding staff permissions. This
     * value may be `0` for events that can run without player participants, but most games should use
     * at least `1`.
     */
    val minPlayersToStart: Int = 1,

    /**
     * Describes the shape of the event for menus, logs and readers of the handler code.
     *
     * The base service does not execute special round hooks based on this value. A value such as
     * [GameMode.BATCHED] or [GameMode.ELIMINATION] means: "the handler will manage this style of
     * game itself, usually with help from reserves and [GameService][dev.slne.surf.survival.events.base.service.GameService]
     * participant-role operations".
     */
    val mode: GameMode = GameMode.ALL_AT_ONCE,

    /**
     * Controls the initial split from online server players into [GameContext.gamePlayers],
     * [GameContext.reservePlayers] and [GameContext.spectators].
     */
    val start: GameStartOptions = GameStartOptions(),

    /**
     * Whether the base service may register players with [ParticipantRole.SPECTATOR].
     *
     * If this is `false`, [StartOverflowPolicy.SPECTATOR], [RunningJoinPolicy.SPECTATOR] and manual
     * spectator joins will be rejected by the base service.
     */
    val spectatorsEnabled: Boolean = true,

    /**
     * Default behaviour for players who join the event server after the session is already running.
     *
     * Use [RunningJoinPolicy.CUSTOM] and override [GameHandler.onRunningJoin] if the decision depends
     * on current event state, for example "late players may join the next heat only while we are still
     * in qualification".
     */
    val runningJoinPolicy: RunningJoinPolicy = RunningJoinPolicy.SPECTATOR,

    /**
     * If `true`, the base join listener may automatically call
     * [GameService.joinRunningEvent(player)][dev.slne.surf.survival.events.base.service.GameService.joinRunningEvent]
     * when a player joins during a running session.
     *
     * The global base config can still disable auto-joining for the whole server.
     */
    val autoJoinRunningPlayers: Boolean = true
) {
    init {
        require(minPlayersToStart >= 0) {
            "minPlayersToStart must not be negative"
        }
    }
}

/**
 * Describes which online players become active immediately when the base session starts.
 *
 * The base service preserves the server join order. With `activePlayerLimit = 10`, the first ten
 * eligible online players become [GameContext.gamePlayers]. All remaining eligible players are
 * handled according to [overflow].
 *
 * This is only the initial split. Later round progression is owned by the event handler.
 */
public data class GameStartOptions(
    /**
     * Maximum number of players that should start as active players.
     *
     * `null` means that every eligible online player starts active. Positive values are useful for
     * races, duels, brackets or any event where only a limited number of players can play at once.
     */
    val activePlayerLimit: Int? = null,

    /**
     * Behaviour for eligible players beyond [activePlayerLimit].
     */
    val overflow: StartOverflowPolicy = StartOverflowPolicy.SPECTATOR
) {
    init {
        require(activePlayerLimit == null || activePlayerLimit > 0) {
            "activePlayerLimit must be positive or null"
        }
    }
}

/** How the base service should register players that do not fit into the initial active set. */
public enum class StartOverflowPolicy {
    /** Overflow players are registered as spectators and receive spectator hooks. */
    SPECTATOR,

    /** Overflow players stay attached as reserves so the handler can activate them later. */
    RESERVE,

    /** Overflow players are not registered in the session. */
    IGNORE
}

/**
 * Human-readable category for an event implementation.
 *
 * This value is intentionally descriptive. It does not force extra handler methods, because many
 * events do not have rounds at all and round formats vary a lot between games.
 */
public enum class GameMode {
    /** Every selected online player participates at once. */
    ALL_AT_ONCE,

    /** Only a limited active set plays while other participants wait or watch. */
    BATCHED,

    /** The handler eliminates players between internally managed rounds. */
    ELIMINATION,

    /** The handler implements behaviour that does not fit the common labels above. */
    CUSTOM
}

/** Default late-join behaviour used by [GameHandler.onRunningJoin]. */
public enum class RunningJoinPolicy {
    /** Players cannot join after the event started. */
    DENY,

    /** Players join as spectators after the event started. */
    SPECTATOR,

    /** Players join as active players after the event started. */
    PLAYER,

    /** Players are attached as reserves after the event started. */
    RESERVE,

    /** The handler decides by overriding [GameHandler.onRunningJoin]. */
    CUSTOM
}

/** Decision returned by [GameHandler.onRunningJoin]. */
public enum class RunningJoinResult {
    /** The player is not added to the running event. */
    DENIED,

    /** The player is added to [GameContext.gamePlayers]. */
    JOINED_AS_PLAYER,

    /** The player is added to [GameContext.reservePlayers]. */
    JOINED_AS_RESERVE,

    /** The player is added to [GameContext.spectators]. */
    JOINED_AS_SPECTATOR
}

/** Current lifecycle state of the base session. */
public enum class GameStatus {
    /** Session exists, [GameHandler.onStarting] is running or about to run. */
    STARTING,

    /** Session is live and join/remove operations are allowed. */
    RUNNING,

    /** Session is being detached and [GameHandler.onStop] is running or about to run. */
    STOPPING
}

/** Why a session is being stopped. */
public enum class GameStopReason {
    /** A manager command stopped the event. */
    COMMAND,

    /** The event handler ended its own session. */
    HANDLER,

    /** The plugin is disabling. */
    PLUGIN_DISABLE,

    /** Startup or handler code failed. */
    ERROR
}

/** Why a player was removed from the current session. */
public enum class PlayerRemoveReason {
    /** The player left voluntarily, usually with `/survivalevents leave`. */
    LEAVE,

    /** A manager or event command removed the player. */
    KICK,

    /** The player disconnected from the server. */
    DISCONNECT,

    /** The whole event stopped. */
    STOPPED
}

/** Role of a UUID inside the base session. */
public enum class ParticipantRole {
    /** Currently active in the game. */
    PLAYER,

    /** Attached to the event but waiting for the handler to activate them. */
    RESERVE,

    /** Watching the event. */
    SPECTATOR
}
