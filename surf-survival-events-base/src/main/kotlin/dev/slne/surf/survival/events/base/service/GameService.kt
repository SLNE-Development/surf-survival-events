package dev.slne.surf.survival.events.base.service

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.config.SurvivalEventsConfig
import dev.slne.surf.survival.events.base.game.*
import dev.slne.surf.survival.events.base.plugin
import dev.slne.surf.survival.events.base.service.GameService.setParticipantRole
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Runtime owner of the currently active event session on the dedicated event server.
 *
 * Event handlers normally interact with this service only for generic operations that should stay
 * consistent across all games: checking the active game, moving participants between active/reserve
 * and spectator roles, removing/kicking players, ending the session and teleporting players back to
 * the shared server lobby.
 *
 * The service deliberately does not implement game-specific rounds. A round event should keep its
 * own bracket/heat state and call [setParticipantRole] when the base snapshot should reflect that a
 * reserve became active or an eliminated player became a spectator.
 */
public object GameService {

    private val lock = ReentrantLock()

    /** Join order of players on the dedicated event server. Used for deterministic start selection. */
    private val serverJoinOrder = ObjectLinkedOpenHashSet<UUID>()

    private var session: GameSession? = null

    internal fun syncOnlinePlayers(players: Iterable<Player> = Bukkit.getOnlinePlayers()) {
        lock.withLock {
            serverJoinOrder.clear()
            players.forEach { serverJoinOrder.add(it.uniqueId) }
        }
    }

    internal suspend fun startGame(key: GameKey<*>): StartGameResult {
        val start = withContext(plugin.globalRegionDispatcher) { // switch context for loading world
            lock.withLock {
                if (session != null) {
                    return@withLock StartUpdate(
                        result = StartGameResult(
                            type = StartGameType.ALREADY_ACTIVE,
                            activeSession = session?.toContext()
                        )
                    )
                }

                val handler = GameRegistry.getRawHandler(key)
                    ?: return@withLock StartUpdate(StartGameResult(StartGameType.NO_HANDLER_REGISTERED))
                val options = handler.options
                val candidates = collectStartCandidateIds()

                if (candidates.size < options.minPlayersToStart) {
                    return@withLock StartUpdate(
                        result = StartGameResult(
                            type = StartGameType.NOT_ENOUGH_PLAYERS,
                            selectedPlayers = candidates.size,
                            minPlayers = options.minPlayersToStart
                        )
                    )
                }

                val eventWorld = try {
                    GameWorldService.loadOrCreateEventWorld(
                        key,
                        handler::customizeWorldCreator,
                        handler::customizeEventWorld
                    )

                } catch (throwable: Throwable) {
                    plugin.logger.log(
                        Level.SEVERE,
                        "Failed to load or create survival event world ${GameWorldService.worldKeyFor(key)}",
                        throwable
                    )
                    return@withLock StartUpdate(
                        StartGameResult(
                            type = StartGameType.WORLD_FAILED,
                            eventWorldKey = GameWorldService.worldKeyFor(key)
                        )
                    )
                }

                val selection = selectPlayers(candidates, options)
                val current = GameSession(
                    key = key,
                    handler = handler,
                    options = options,
                    status = GameStatus.STARTING,
                    eventWorld = eventWorld,
                    gamePlayers = selection.gamePlayers.toMutableLinkedSet(),
                    reservePlayers = selection.reservePlayers.toMutableLinkedSet(),
                    spectators = selection.spectators.toMutableSet()
                )

                session = current

                StartUpdate(
                    result = StartGameResult(
                        type = StartGameType.STARTED,
                        context = current.toContext(),
                        selectedPlayers = candidates.size,
                        minPlayers = options.minPlayersToStart
                    ),
                    session = current,
                    handler = handler,
                    startingContext = current.toContext()
                )
            }
        }

        if (start.result.type != StartGameType.STARTED) {
            return start.result
        }

        val current = start.session ?: error("Missing started session")
        val handler = start.handler ?: error("Missing game handler")
        val startingContext = start.startingContext ?: error("Missing starting context")

        return try {
            context(startingContext) {
                handler.onStarting()
            }

            val runningContext = lock.withLock {
                if (session !== current) {
                    return StartGameResult(StartGameType.NO_ACTIVE_GAME)
                }

                current.status = GameStatus.RUNNING
                current.toContext()
            }

            context(runningContext) {
                handler.onStarted()
            }

            val result = StartGameResult(
                type = StartGameType.STARTED,
                context = runningContext,
                selectedPlayers = runningContext.playerCount,
                minPlayers = runningContext.options.minPlayersToStart
            )

            if (SurvivalEventsConfig.getConfig().start.announceStart) {
                AnnouncementService.broadcastStartedEvent(runningContext)
            }

            result
        } catch (e: Throwable) {
            plugin.logger.log(Level.SEVERE, "Failed to start survival event ${key.displayName}", e)
            val failed = detachSpecificSession(current, GameStopReason.ERROR)
            failed?.let { stopped ->
                safeLaunchStopHook(stopped, GameStopReason.ERROR)
            }
            StartGameResult(StartGameType.HANDLER_FAILED)
        }
    }

    /**
     * Stops the active session asynchronously and returns the stopped game key, or `null` if no
     * session is active.
     *
     * The handler's [GameHandler.onStop] hook is launched and failures are logged.
     */
    public fun stopGame(reason: GameStopReason = GameStopReason.COMMAND): GameKey<*>? {
        val stopped = detachSession(reason) ?: return null
        launchHook {
            context(stopped.context) {
                stopped.handler.onStop(reason)
            }
        }
        return stopped.context.key
    }

    /**
     * Stops the active session and waits until [GameHandler.onStop] has finished.
     *
     * Use this from plugin disable hooks or commands that need deterministic cleanup before
     * continuing.
     */
    public suspend fun stopGameAndWait(reason: GameStopReason = GameStopReason.COMMAND): GameKey<*>? {
        val stopped = detachSession(reason) ?: return null
        safeRunStopHook(stopped, reason)
        return stopped.context.key
    }

    /** Convenience alias for handlers that want to end their own event session. */
    public suspend fun endGame(reason: GameStopReason = GameStopReason.HANDLER): GameKey<*>? {
        return stopGameAndWait(reason)
    }

    public fun isGameActive(): Boolean {
        return getActiveGameKeyOrNull() != null
    }

    public fun isGameRunning(): Boolean {
        return lock.withLock { session?.status == GameStatus.RUNNING }
    }

    public fun isActiveGame(key: GameKey<*>): Boolean {
        return getActiveGameKeyOrNull() == key
    }

    public fun getActiveGameKeyOrNull(): GameKey<*>? {
        return lock.withLock { session?.key }
    }

    public fun getActiveGameKey(): GameKey<*> {
        return getActiveGameKeyOrNull() ?: error("No active game found")
    }

    /**
     * Returns an immutable snapshot of the current session, or `null` if no event is active.
     *
     * The returned context will not update automatically. Call this again after joins, kicks or role
     * changes if current state matters.
     */
    public fun snapshot(): GameContext? {
        return lock.withLock { session?.toContext() }
    }

    public fun requiredSnapshot(expectedGame: GameKey<*>): GameContext {
        return lock.withLock {
            val current = session ?: error("No active game found")
            if (current.key != expectedGame) {
                error("Expected game ${expectedGame.displayName} but found ${current.key.displayName}")
            }
            current.toContext()
        }
    }

    @OptIn(ExperimentalContracts::class)
    public inline fun <R> withGameContext(expectedGame: GameKey<*>, block: (context(GameContext) () -> R)): R {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        return context(requiredSnapshot(expectedGame), block)
    }

    public fun getStartCandidateIds(): List<UUID> {
        return lock.withLock { collectStartCandidateIds() }
    }

    internal fun onPlayerJoin(player: Player) {
        val context = lock.withLock {
            serverJoinOrder.add(player.uniqueId)
            session?.toContext()
        }

        if (context == null) {
            if (SurvivalEventsConfig.getConfig().join.teleportToServerLobbyWhenIdle) {
                plugin.launch { teleportToServerLobby(player) }
            }
            return
        }

        when (context.status) {
            GameStatus.STARTING -> {
                player.sendText {
                    appendInfoPrefix()
                    info("Das Event startet gerade. Bitte warte einen Moment.")
                }
            }

            GameStatus.RUNNING -> {
                val joinConfig = SurvivalEventsConfig.getConfig().join
                if (context.options.autoJoinRunningPlayers && joinConfig.autoJoinRunningEvent) {
                    plugin.launch {
                        val result = joinRunningEvent(player)
                        if (!result.joined && joinConfig.announceRunningEventOnJoin) {
                            AnnouncementService.sendRunningEvent(player, context.key)
                        }
                    }
                } else if (joinConfig.announceRunningEventOnJoin) {
                    AnnouncementService.sendRunningEvent(player, context.key)
                }
            }

            GameStatus.STOPPING -> Unit
        }
    }

    internal fun onPlayerQuit(player: Player) {
        lock.withLock {
            serverJoinOrder.remove(player.uniqueId)
        }
        remove(player.uniqueId, includeSpectators = true, reason = PlayerRemoveReason.DISCONNECT)
    }

    /**
     * Lets the active handler decide how [player] should join the already running event.
     *
     * This is used by the join listener when auto-join is enabled and can also be called by custom
     * commands. The matching handler hook is called after the base service registered the player.
     */
    public suspend fun joinRunningEvent(player: Player): JoinEventResult {
        val uuid = player.uniqueId
        val request = lock.withLock {
            val current = session ?: return@withLock RunningJoinRequest(
                result = JoinEventResult(JoinEventType.NO_ACTIVE_GAME)
            )

            when {
                current.status == GameStatus.STARTING || current.status == GameStatus.STOPPING -> {
                    RunningJoinRequest(result = JoinEventResult(JoinEventType.EVENT_BUSY))
                }

                current.status != GameStatus.RUNNING -> {
                    RunningJoinRequest(result = JoinEventResult(JoinEventType.EVENT_BUSY))
                }

                current.isParticipant(uuid) -> {
                    RunningJoinRequest(result = JoinEventResult(JoinEventType.ALREADY_PARTICIPATING))
                }

                uuid in current.spectators -> {
                    RunningJoinRequest(result = JoinEventResult(JoinEventType.ALREADY_SPECTATOR))
                }

                else -> RunningJoinRequest(
                    session = current,
                    handler = current.handler,
                    context = current.toContext()
                )
            }
        }

        request.result?.let { return it }

        val current = request.session ?: error("Missing running session")
        val handler = request.handler ?: error("Missing running handler")
        val decision = try {
            context(request.context ?: current.toContext()) {
                handler.onRunningJoin(player)
            }
        } catch (throwable: Throwable) {
            plugin.componentLogger.error("Failed to decide running join for ${player.name}", throwable)
            RunningJoinResult.DENIED
        }

        val update = lock.withLock {
            if (session !== current || current.status != GameStatus.RUNNING) {
                return@withLock JoinUpdate(JoinEventResult(JoinEventType.EVENT_BUSY))
            }

            when {
                current.isParticipant(uuid) -> JoinUpdate(JoinEventResult(JoinEventType.ALREADY_PARTICIPATING))
                uuid in current.spectators -> JoinUpdate(JoinEventResult(JoinEventType.ALREADY_SPECTATOR))
                decision == RunningJoinResult.JOINED_AS_PLAYER -> {
                    current.gamePlayers.add(uuid)
                    JoinUpdate(
                        result = JoinEventResult(JoinEventType.JOINED_AS_PLAYER, ParticipantRole.PLAYER),
                        handler = current.handler,
                        context = current.toContext(),
                        player = player,
                        role = ParticipantRole.PLAYER
                    )
                }

                decision == RunningJoinResult.JOINED_AS_RESERVE -> {
                    current.reservePlayers.add(uuid)
                    JoinUpdate(
                        result = JoinEventResult(JoinEventType.JOINED_AS_RESERVE, ParticipantRole.RESERVE),
                        handler = current.handler,
                        context = current.toContext(),
                        player = player,
                        role = ParticipantRole.RESERVE
                    )
                }

                decision == RunningJoinResult.JOINED_AS_SPECTATOR && current.options.spectatorsEnabled -> {
                    current.spectators.add(uuid)
                    JoinUpdate(
                        result = JoinEventResult(JoinEventType.JOINED_AS_SPECTATOR, ParticipantRole.SPECTATOR),
                        handler = current.handler,
                        context = current.toContext(),
                        player = player,
                        role = ParticipantRole.SPECTATOR
                    )
                }

                decision == RunningJoinResult.JOINED_AS_SPECTATOR -> JoinUpdate(
                    JoinEventResult(JoinEventType.SPECTATORS_DISABLED)
                )

                else -> JoinUpdate(JoinEventResult(JoinEventType.RUNNING_JOIN_DENIED))
            }
        }

        dispatchJoinUpdateAwait(update)
        return update.result
    }

    /** Registers [player] as a spectator in the active session if spectators are enabled. */
    public suspend fun joinSpectator(player: Player): JoinEventResult {
        val update = addSpectator(player)
        dispatchJoinUpdateAwait(update)
        return update.result
    }

    /** Removes [player] from the active session and notifies the handler. */
    public fun remove(player: Player, includeSpectators: Boolean = true): RemoveResult {
        return remove(player.uniqueId, includeSpectators)
    }

    public fun remove(
        uuid: UUID,
        includeSpectators: Boolean = true,
        reason: PlayerRemoveReason = PlayerRemoveReason.LEAVE
    ): RemoveResult {
        val update = lock.withLock {
            val current = session ?: return@withLock RemoveUpdate(RemoveResult.NO_ACTIVE_GAME)
            val player = Bukkit.getPlayer(uuid)

            when {
                current.status == GameStatus.STARTING || current.status == GameStatus.STOPPING -> {
                    RemoveUpdate(RemoveResult.EVENT_BUSY)
                }

                current.gamePlayers.remove(uuid) -> RemoveUpdate(
                    result = RemoveResult.REMOVED_PLAYER,
                    handler = current.handler,
                    context = current.toContext(),
                    uuid = uuid,
                    player = player,
                    role = ParticipantRole.PLAYER,
                    reason = reason
                )

                current.reservePlayers.remove(uuid) -> RemoveUpdate(
                    result = RemoveResult.REMOVED_RESERVE,
                    handler = current.handler,
                    context = current.toContext(),
                    uuid = uuid,
                    player = player,
                    role = ParticipantRole.RESERVE,
                    reason = reason
                )

                includeSpectators && current.spectators.remove(uuid) -> RemoveUpdate(
                    result = RemoveResult.REMOVED_SPECTATOR,
                    handler = current.handler,
                    context = current.toContext(),
                    uuid = uuid,
                    player = player,
                    role = ParticipantRole.SPECTATOR,
                    reason = reason
                )

                else -> RemoveUpdate(RemoveResult.NOT_PARTICIPATING)
            }
        }

        dispatchRemoveUpdate(update)
        return update.result
    }

    /** Removes [uuid] with [PlayerRemoveReason.KICK]. */
    public fun kick(uuid: UUID, includeSpectators: Boolean = false): RemoveResult {
        return remove(uuid, includeSpectators, PlayerRemoveReason.KICK)
    }

    /**
     * Moves an already registered event UUID to [role].
     *
     * This is the main base-level helper for batched or round events. It only changes the generic
     * session role; the event remains responsible for teleporting, giving items and updating its own
     * scoreboard/progress state.
     */
    public fun setParticipantRole(uuid: UUID, role: ParticipantRole): MoveParticipantResult {
        val update = lock.withLock {
            val current = session ?: return@withLock RoleChangeUpdate(MoveParticipantResult.NO_ACTIVE_GAME)
            if (current.status != GameStatus.RUNNING) return@withLock RoleChangeUpdate(MoveParticipantResult.EVENT_BUSY)
            if (role == ParticipantRole.SPECTATOR && !current.options.spectatorsEnabled) {
                return@withLock RoleChangeUpdate(MoveParticipantResult.SPECTATORS_DISABLED)
            }

            val currentRole = current.roleOf(uuid)
                ?: return@withLock RoleChangeUpdate(MoveParticipantResult.NOT_PARTICIPATING)
            if (currentRole == role) return@withLock RoleChangeUpdate(MoveParticipantResult.ALREADY_IN_ROLE)

            current.removeFromAll(uuid)
            current.addToRole(uuid, role)
            RoleChangeUpdate(
                result = MoveParticipantResult.MOVED,
                handler = current.handler,
                context = current.toContext(),
                uuid = uuid,
                previousRole = currentRole,
                newRole = role
            )
        }

        dispatchRoleChangeUpdate(update)
        return update.result
    }

    public fun isParticipant(player: Player): Boolean {
        return isParticipant(player.uniqueId)
    }

    public fun isParticipant(uuid: UUID): Boolean {
        return lock.withLock { session?.isParticipant(uuid) ?: false }
    }

    public fun isPlayer(uuid: UUID): Boolean {
        return lock.withLock { uuid in (session?.gamePlayers ?: return@withLock false) }
    }

    public fun isReserve(uuid: UUID): Boolean {
        return lock.withLock { uuid in (session?.reservePlayers ?: return@withLock false) }
    }

    public fun isSpectator(uuid: UUID): Boolean {
        return lock.withLock { uuid in (session?.spectators ?: return@withLock false) }
    }

    public fun participantPosition(uuid: UUID): ParticipantPosition? {
        return lock.withLock {
            val current = session ?: return@withLock null

            when (uuid) {
                in current.gamePlayers -> ParticipantPosition(
                    role = ParticipantRole.PLAYER,
                    position = current.gamePlayers.indexOf(uuid) + 1,
                    size = current.gamePlayers.size
                )

                in current.reservePlayers -> ParticipantPosition(
                    role = ParticipantRole.RESERVE,
                    position = current.reservePlayers.indexOf(uuid) + 1,
                    size = current.reservePlayers.size
                )

                in current.spectators -> ParticipantPosition(
                    role = ParticipantRole.SPECTATOR,
                    position = -1,
                    size = current.spectators.size
                )

                else -> null
            }
        }
    }

    private fun collectStartCandidateIds(): List<UUID> {
        require(lock.isHeldByCurrentThread) { "Must be called from the game thread" }

        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        val onlineById = onlinePlayers.associateBy { it.uniqueId }

        serverJoinOrder.removeAll { it !in onlineById }
        onlinePlayers.forEach { serverJoinOrder.add(it.uniqueId) }

        return serverJoinOrder
            .asSequence()
            .mapNotNull { onlineById[it] }
            .filter(::shouldUseAsStartCandidate)
            .map { it.uniqueId }
            .toList()
    }

    private fun shouldUseAsStartCandidate(player: Player): Boolean {
        val config = SurvivalEventsConfig.getConfig().start

        return config.excludedPermissions.none { permission ->
            permission.isNotBlank() && player.hasPermission(permission)
        }
    }

    private fun selectPlayers(candidates: List<UUID>, options: GameOptions): PlayerSelection {
        val limit = options.start.activePlayerLimit
        if (limit == null || candidates.size <= limit) {
            return PlayerSelection(gamePlayers = candidates)
        }

        val active = candidates.take(limit)
        val overflow = candidates.drop(limit)

        return when (options.start.overflow) {
            StartOverflowPolicy.SPECTATOR -> if (options.spectatorsEnabled) {
                PlayerSelection(
                    gamePlayers = active,
                    spectators = overflow.toSet()
                )
            } else {
                PlayerSelection(gamePlayers = active)
            }

            StartOverflowPolicy.RESERVE -> PlayerSelection(
                gamePlayers = active,
                reservePlayers = overflow
            )

            StartOverflowPolicy.IGNORE -> PlayerSelection(gamePlayers = active)
        }
    }

    private fun addSpectator(player: Player): JoinUpdate {
        val uuid = player.uniqueId

        return lock.withLock {
            val current = session ?: return@withLock JoinUpdate(JoinEventResult(JoinEventType.NO_ACTIVE_GAME))

            when {
                current.status == GameStatus.STARTING || current.status == GameStatus.STOPPING -> {
                    JoinUpdate(JoinEventResult(JoinEventType.EVENT_BUSY))
                }

                !current.options.spectatorsEnabled -> JoinUpdate(JoinEventResult(JoinEventType.SPECTATORS_DISABLED))
                current.isParticipant(uuid) -> JoinUpdate(JoinEventResult(JoinEventType.ALREADY_PARTICIPATING))
                uuid in current.spectators -> JoinUpdate(JoinEventResult(JoinEventType.ALREADY_SPECTATOR))
                else -> {
                    current.spectators.add(uuid)
                    JoinUpdate(
                        result = JoinEventResult(JoinEventType.JOINED_AS_SPECTATOR, ParticipantRole.SPECTATOR),
                        handler = current.handler,
                        context = current.toContext(),
                        player = player,
                        role = ParticipantRole.SPECTATOR
                    )
                }
            }
        }
    }

    private suspend fun dispatchJoinUpdateAwait(update: JoinUpdate) {
        val handler = update.handler ?: return
        val context = update.context ?: return
        val player = update.player ?: return
        context(context) {
            when (update.role ?: return) {
                ParticipantRole.PLAYER -> handler.onRunningPlayerJoin(player)
                ParticipantRole.RESERVE -> handler.onRunningReserveJoin(player)
                ParticipantRole.SPECTATOR -> handler.onRunningSpectatorJoin(player)
            }
        }
    }

    private fun dispatchRoleChangeUpdate(update: RoleChangeUpdate) {
        val handler = update.handler ?: return
        val context = update.context ?: return
        val uuid = update.uuid ?: return
        val previousRole = update.previousRole ?: return
        val newRole = update.newRole ?: return

        launchHook {
            context(context) {
                handler.onParticipantRoleChange(uuid, previousRole, newRole)
            }
        }
    }

    private fun dispatchRemoveUpdate(update: RemoveUpdate) {
        val handler = update.handler ?: return
        val context = update.context ?: return
        val uuid = update.uuid ?: return
        val role = update.role ?: return
        val reason = update.reason ?: return

        launchHook {
            context(context) {
                handler.onParticipantRemove(uuid, update.player, role, reason)
            }
        }
    }

    private fun detachSession(reason: GameStopReason): StoppedGame? {
        return lock.withLock {
            val current = session ?: return@withLock null
            current.status = GameStatus.STOPPING

            val stopped = StoppedGame(
                handler = current.handler,
                context = current.toContext()
            )

            session = null
            stopped
        }
    }

    private fun detachSpecificSession(current: GameSession, reason: GameStopReason): StoppedGame? {
        return lock.withLock {
            if (session !== current) return@withLock null
            current.status = GameStatus.STOPPING

            val stopped = StoppedGame(
                handler = current.handler,
                context = current.toContext()
            )

            session = null
            stopped
        }
    }

    private suspend fun safeRunStopHook(stopped: StoppedGame, reason: GameStopReason) {
        try {
            context(stopped.context) {
                stopped.handler.onStop(reason)
            }
        } catch (throwable: Throwable) {
            plugin.componentLogger.error("Survival event stop hook failed", throwable)
        }
    }

    private fun safeLaunchStopHook(stopped: StoppedGame, reason: GameStopReason) {
        launchHook {
            context(stopped.context) {
                stopped.handler.onStop(reason)
            }
        }
    }

    private fun launchHook(block: suspend () -> Unit) {
        plugin.launch {
            try {
                block()
            } catch (throwable: Throwable) {
                plugin.componentLogger.error("Survival event hook failed", throwable)
            }
        }
    }

    /**
     * Teleports [player] to the configured server lobby and returns whether Bukkit accepted the
     * teleport.
     *
     * Event handlers can use this from remove/stop hooks to send players back to the shared event
     * server lobby. Disconnected players should not be teleported; check [PlayerRemoveReason] in
     * [GameHandler.onParticipantRemove] before calling this helper.
     */
    public suspend fun teleportToServerLobby(player: Player): Boolean {
        return try {
            player.teleportAsync(SurvivalEventsConfig.getConfig().serverLobby).await()
        } catch (throwable: Throwable) {
            plugin.componentLogger.error("Failed to teleport ${player.name} to the event server lobby", throwable)
            false
        }
    }

    internal data class StartGameResult(
        val type: StartGameType,
        val context: GameContext? = null,
        val activeSession: GameContext? = null,
        val selectedPlayers: Int = 0,
        val minPlayers: Int = 0,
        val eventWorldKey: NamespacedKey? = null
    ) {
        val started: Boolean
            get() = type == StartGameType.STARTED
    }

    internal enum class StartGameType {
        STARTED,
        ALREADY_ACTIVE,
        NO_ACTIVE_GAME,
        NO_HANDLER_REGISTERED,
        NOT_ENOUGH_PLAYERS,
        WORLD_FAILED,
        HANDLER_FAILED
    }

    public data class JoinEventResult(
        val type: JoinEventType,
        val role: ParticipantRole? = null
    ) {
        val joined: Boolean
            get() = type.joined
    }

    public enum class JoinEventType(
        public val joined: Boolean
    ) {
        NO_ACTIVE_GAME(false),
        EVENT_BUSY(false),
        RUNNING_JOIN_DENIED(false),
        SPECTATORS_DISABLED(false),
        ALREADY_PARTICIPATING(false),
        ALREADY_SPECTATOR(false),
        JOINED_AS_PLAYER(true),
        JOINED_AS_RESERVE(true),
        JOINED_AS_SPECTATOR(true)
    }

    public enum class RemoveResult(
        public val removed: Boolean
    ) {
        NO_ACTIVE_GAME(false),
        EVENT_BUSY(false),
        NOT_PARTICIPATING(false),
        REMOVED_PLAYER(true),
        REMOVED_RESERVE(true),
        REMOVED_SPECTATOR(true)
    }

    public enum class MoveParticipantResult {
        NO_ACTIVE_GAME,
        EVENT_BUSY,
        NOT_PARTICIPATING,
        SPECTATORS_DISABLED,
        ALREADY_IN_ROLE,
        MOVED
    }

    public data class ParticipantPosition(
        val role: ParticipantRole,
        val position: Int,
        val size: Int
    )

    private class GameSession(
        val key: GameKey<*>,
        val handler: GameHandler,
        val options: GameOptions,
        var status: GameStatus,
        val eventWorld: World,
        val gamePlayers: MutableSet<UUID>,
        val reservePlayers: MutableSet<UUID>,
        val spectators: MutableSet<UUID>
    ) {
        fun toContext() = GameContext(
            key = key,
            options = options,
            status = status,
            eventWorld = eventWorld,
            gamePlayers = gamePlayers.toList(),
            reservePlayers = reservePlayers.toList(),
            spectators = spectators.toSet()
        )

        fun isParticipant(uuid: UUID): Boolean {
            return uuid in gamePlayers || uuid in reservePlayers
        }

        fun roleOf(uuid: UUID): ParticipantRole? {
            return when (uuid) {
                in gamePlayers -> ParticipantRole.PLAYER
                in reservePlayers -> ParticipantRole.RESERVE
                in spectators -> ParticipantRole.SPECTATOR
                else -> null
            }
        }

        fun removeFromAll(uuid: UUID) {
            gamePlayers.remove(uuid)
            reservePlayers.remove(uuid)
            spectators.remove(uuid)
        }

        fun addToRole(uuid: UUID, role: ParticipantRole) {
            when (role) {
                ParticipantRole.PLAYER -> gamePlayers.add(uuid)
                ParticipantRole.RESERVE -> reservePlayers.add(uuid)
                ParticipantRole.SPECTATOR -> spectators.add(uuid)
            }
        }
    }

    private data class PlayerSelection(
        val gamePlayers: List<UUID> = emptyList(),
        val reservePlayers: List<UUID> = emptyList(),
        val spectators: Set<UUID> = emptySet()
    )

    private data class StartUpdate(
        val result: StartGameResult,
        val session: GameSession? = null,
        val handler: GameHandler? = null,
        val startingContext: GameContext? = null
    )

    private data class RunningJoinRequest(
        val result: JoinEventResult? = null,
        val session: GameSession? = null,
        val handler: GameHandler? = null,
        val context: GameContext? = null
    )

    private data class JoinUpdate(
        val result: JoinEventResult,
        val handler: GameHandler? = null,
        val context: GameContext? = null,
        val player: Player? = null,
        val role: ParticipantRole? = null
    )

    private data class RoleChangeUpdate(
        val result: MoveParticipantResult,
        val handler: GameHandler? = null,
        val context: GameContext? = null,
        val uuid: UUID? = null,
        val previousRole: ParticipantRole? = null,
        val newRole: ParticipantRole? = null
    )

    private data class RemoveUpdate(
        val result: RemoveResult,
        val handler: GameHandler? = null,
        val context: GameContext? = null,
        val uuid: UUID? = null,
        val player: Player? = null,
        val role: ParticipantRole? = null,
        val reason: PlayerRemoveReason? = null
    )

    private data class StoppedGame(
        val handler: GameHandler,
        val context: GameContext
    )
}

private fun Iterable<UUID>.toMutableLinkedSet(): MutableSet<UUID> = linkedSetOf<UUID>().also { target ->
    target.addAll(this)
}
