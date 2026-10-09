package dev.slne.surf.survival.events.werewolf.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.plain
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.werewolf.dialog.WerewolfRoleViewDialoge
import dev.slne.surf.survival.events.werewolf.domain.WerewolfGameEngine
import dev.slne.surf.survival.events.werewolf.messaging.WerewolfMessenger
import dev.slne.surf.survival.events.werewolf.plugin
import dev.slne.surf.survival.events.werewolf.scoreboard.addToWerewolfScoreboard
import dev.slne.surf.survival.events.werewolf.scoreboard.removeFromWerewolfScoreboard
import dev.slne.surf.survival.events.werewolf.scoreboard.updateWerewolfScoreboards
import dev.slne.surf.survival.events.werewolf.service.HiddenPlayerPair
import dev.slne.surf.survival.events.werewolf.service.WerewolfVisibilityCleanup
import dev.slne.surf.survival.events.werewolf.util.*
import dev.slne.surf.survival.events.werewolf.voicechat.WerewolfVoicechatPlugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.plusAssign
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed class WerewolfJoinResult {
    data object Success : WerewolfJoinResult()
    data object AlreadyInGame : WerewolfJoinResult()
    data object AlreadyStarted : WerewolfJoinResult()
    data class Error(val message: String) : WerewolfJoinResult()
}

sealed class WerewolfStartResult {
    data object Success : WerewolfStartResult()
    data object NotInLobbyPhase : WerewolfStartResult()
    data class NotEnoughPlayers(val current: Int, val required: Int) : WerewolfStartResult()
    data class Error(val message: String) : WerewolfStartResult()
}

class WerewolfService(val gameId: String) {

    companion object {
        const val MIN_PLAYERS = 8
    }

    internal val lock: Any = Any()

    internal val players: MutableMap<UUID, WerewolfPlayer> = ConcurrentHashMap()

    val leader: UUID?
        get() = synchronized(lock) { _leader }

    private var _leader: UUID? = null

    val phase: GamePhase
        get() = synchronized(lock) { _phase }

    private var _phase = GamePhase.IDLE

    val state: GameState
        get() = synchronized(lock) { _state }

    private var _state = GameState.DAY

    val isPhaseTransitioning: Boolean
        get() = synchronized(lock) { _isPhaseTransitioning }

    private var _isPhaseTransitioning = false

    private var werewolfTask: Job? = null
    private var werewolfUiTask: Job? = null

    val werewolfTime: Duration
        get() = werewolfStartedAt?.elapsedNow() ?: _werewolfTime

    private var _werewolfTime: Duration = 0.seconds
    private var werewolfStartedAt: TimeMark? = null

    private val messenger = WerewolfMessenger(this)
    private var privateWerewolfVoiceChatActive = false

    val aliveCount: Int
        get() = getAlivePlayers().size

    val totalCount: Int
        get() = players.size

    fun playerUuids(): Set<UUID> = players.keys.toSet()

    private val audioHandler = WerewolfVoicechatPlugin.getAudioHandler(gameId)

    val engine: WerewolfGameEngine
        get() =_engine

    @Volatile
    private var phaseSessionId = 0

    private var _engine = WerewolfGameEngine(this)
    private val glowingTargetsByWerewolf = Collections.synchronizedMap(mutableMapOf<UUID, UUID>())
    private val glowingTargetsByWitch = Collections.synchronizedMap(mutableMapOf<UUID, Set<UUID>>())
    private val hiddenPlayerPairs = Collections.synchronizedSet(mutableSetOf<HiddenPlayerPair>())
    private val pendingNightExecutions = Collections.synchronizedList(mutableListOf<UUID>())
    private val phaseTransitionDelay = 3.seconds
    private var stopRequested = false

    fun openLobby(leaderUuid: UUID?): Unit = synchronized(lock) {
        if (_phase != GamePhase.IDLE) return@synchronized

        players.clear()
        _phase = GamePhase.LOBBY
        _leader = leaderUuid
        _werewolfTime = 0.seconds
        werewolfStartedAt = null
        stopRequested = false
    }

    suspend fun join(uuid: UUID): WerewolfJoinResult {
        val precheck = synchronized(lock) { checkJoinable(uuid) }
        if (precheck != null) return precheck

        val player = uuid.toBukkitPlayer()
            ?: return WerewolfJoinResult.Error("Dein Spieler konnte nicht gefunden werden!")

        val previousGameMode = withContext(plugin.entityDispatcher(player)) {
            player.addToWerewolfScoreboard()
            player.gameMode
        }

        val joined = synchronized(lock) {
            if (checkJoinable(uuid) != null) return@synchronized false
            players[uuid] = WerewolfPlayer(uuid, previousGameMode = previousGameMode)
            true
        }

        if (!joined) {
            withContext(plugin.entityDispatcher(player)) { player.removeFromWerewolfScoreboard() }
            return synchronized(lock) { checkJoinable(uuid) } ?: WerewolfJoinResult.AlreadyInGame
        }

        announceToAll {
            appendSuccessPrefix()
            variableValue(player.name)
            appendSpace()
            success("ist der Werwolf-Runde beigetreten!")
        }
        return WerewolfJoinResult.Success
    }

    private fun checkJoinable(uuid: UUID): WerewolfJoinResult? {
        if (_phase != GamePhase.LOBBY) return WerewolfJoinResult.AlreadyStarted
        if (players.containsKey(uuid)) return WerewolfJoinResult.AlreadyInGame
        return null
    }

    suspend fun start(): WerewolfStartResult {
        val prepared = synchronized(lock) {
            if (werewolfTask != null) error("Werewolf task is already running!")
            if (werewolfUiTask != null) error("Werewolf UI task is already running!")

            if (_phase != GamePhase.LOBBY) {
                return@synchronized StartPreparation.Failure(WerewolfStartResult.NotInLobbyPhase)
            }

            if (players.size < MIN_PLAYERS) {
                return@synchronized StartPreparation.Failure(
                    WerewolfStartResult.NotEnoughPlayers(players.size, MIN_PLAYERS)
                )
            }

            try {
                stopRequested = false
                phaseSessionId += 1
                _phase = GamePhase.RUNNING
                _state = GameState.DAY
                _werewolfTime = 0.seconds
                werewolfStartedAt = TimeSource.Monotonic.markNow()
                pendingNightExecutions.clear()
                restoreParticipantVisibility()
                WerewolfVisibilityCleanup.restorePendingForPlayers(allParticipantIds())
                val roleMap = WerewolfRoleSelection.assignRoles(players.keys.toList())

                this._engine.startGameEngine()

                roleMap.forEach { (uuid, role) ->
                    players[uuid]?.role = role
                }

                messenger.announceLeaderRoles(roleMap)
                werewolfUiTask = launchWerewolfUiTask()
                startWerewolfTickTask()

                StartPreparation.Success(roleMap)
            } catch (e: Exception) {
                werewolfTask?.cancel(CancellationException("Werewolf game '$gameId' failed to start"))
                werewolfTask = null
                werewolfUiTask?.cancel(CancellationException("Werewolf game '$gameId' failed to start"))
                werewolfUiTask = null
                werewolfStartedAt = null
                _phase = GamePhase.IDLE
                StartPreparation.Failure(WerewolfStartResult.Error(e.message ?: "Unbekannter Fehler beim Starten"))
            }
        }

        val roleMap = when (prepared) {
            is StartPreparation.Failure -> return prepared.result
            is StartPreparation.Success -> prepared.roleMap
        }

        roleMap.forEach { (uuid, role) ->
            val player = uuid.toBukkitPlayer() ?: return@forEach
            withContext(plugin.entityDispatcher(player)) {
                sendRoleStartMessage(player, role)
                player.showDialog(WerewolfRoleViewDialoge.create(player))
            }
        }

        messenger.announceGameStarted()
        players.keys.toList().forEach { uuid ->
            val player = uuid.toBukkitPlayer() ?: return@forEach
            withContext(plugin.entityDispatcher(player)) {
                player.showTitle {
                    title { primary("Werwolf gestartet") }
                    subtitle { variableValue("Viel Spaß!") }
                    times {
                        fadeIn(500.milliseconds)
                        stay(3.seconds)
                        fadeOut(500.milliseconds)
                    }
                }
                player.playSound(true) {
                    type(Sound.BLOCK_BEACON_ACTIVATE)
                    volume(.5f)
                    pitch(.5f)
                }
            }
        }

        refreshCommandRequirements()
        return WerewolfStartResult.Success
    }

    private sealed class StartPreparation {
        data class Success(val roleMap: Map<UUID, WerwolfRoles>) : StartPreparation()
        data class Failure(val result: WerewolfStartResult) : StartPreparation()
    }

    private fun startWerewolfTickTask() {
        werewolfTask = plugin.launch {
                while (isActive) {
                    if (state == GameState.NIGHT) {
                        getAlivePlayers().forEach { werewolfPlayer ->
                            val player = werewolfPlayer.uuid.toBukkitPlayer() ?: return@forEach

                            withContext(plugin.entityDispatcher(player)) {
                                if (engine.canRoleActAtNight(werewolfPlayer.role)) {
                                    player.removePotionEffect(PotionEffectType.BLINDNESS)
                                } else {
                                    player.addPotionEffect(
                                        PotionEffect(
                                            PotionEffectType.BLINDNESS,
                                            100,
                                            0,
                                            false,
                                            false,
                                            false
                                        )
                                    )
                                }

                                if (engine.currentNightStep == NightStep.WEREWOLVES &&
                                    werewolfPlayer.role == WerwolfRoles.WERWOLF
                                ) {
                                    makeGlowing(player)
                                }

                                if (engine.currentNightStep == NightStep.WITCH &&
                                    werewolfPlayer.role == WerwolfRoles.WITCH
                                ) {
                                    makeWitchGlowing(player)
                                }
                            }
                        }

                        if (engine.currentNightStep != NightStep.WEREWOLVES) {
                            clearWerewolfGlowingNow()
                        }

                        if (engine.currentNightStep != NightStep.WITCH) {
                            clearWitchGlowingNow()
                        }
                    } else {
                        clearBlindnessNow()
                        clearWerewolfGlowingNow()
                        clearWitchGlowingNow()
                    }

                    delay(1.seconds)

                    //Chek if Phase is over
                    val advanceResult = engine.tick()

                    if (advanceResult != null) {
                        if (advanceResult.winner != null) {
                            executePendingNightExecutionsBeforeGameEnd()
                            finishGame(advanceResult.winner)
                            return@launch
                        }

                        waitForPhaseTransition()

                        when (advanceResult.nextPhase) {
                            GameState.NIGHT -> {
                                messenger.announcePhaseStarted(GameState.NIGHT)
                                engine.announceCurrentNightStep()
                            }

                            GameState.DAY -> {
                                messenger.announceDayStarted(pendingNightExecutions.toList())
                                executePendingNightExecutions()
                            }

                            GameState.VOTE -> Unit

                            GameState.MAYOR_VOTE -> Unit
                        }

                        refreshCommandRequirements()
                    }
                }
        }
    }

    private fun sendRoleStartMessage(player: Player, role: WerwolfRoles) {
        player.sendText {
            appendInfoPrefix()
            appendRoleMessageHeader(role)
        }

        player.sendText {
            appendInfoPrefix()
            appendRoleMessageLine("Aufgabe", roleDescriptionText(role))
        }

        player.sendText {
            appendInfoPrefix()
            appendRoleMessageLine("Tipp", roleStartHint(role))
        }

        player.sendText {
            appendInfoPrefix()
            appendRoleMessageLine(
                "Aktionen",
                "Klickbare Commands erscheinen automatisch, wenn du dran bist."
            )
        }

        player.sendText {
            appendInfoPrefix()
            appendRoleMessageHeader(role)
        }
    }

    private fun launchWerewolfUiTask(): Job = plugin.launch {
        while (isActive) {
            updateWerewolfScoreboards()

            allParticipants.forEach { participant ->
                withContext(plugin.entityDispatcher(participant)) {
                    participant.sendActionBar(
                        buildText {
                            primary("Es ist ")
                            append(state.displayName)
                        }
                    )
                }
            }

            delay(500.milliseconds)
        }
    }

    private fun SurfComponentBuilder.appendRoleMessageHeader(role: WerwolfRoles) {
        spacer("-----")
        appendSpace()
        gold("Deine Rolle", TextDecoration.BOLD)
        appendSpace()
        spacer("-")
        appendSpace()
        append(role.displayName)
        appendSpace()
        spacer("-----")
    }

    private fun SurfComponentBuilder.appendRoleMessageLine(
        label: String,
        text: String,
    ) {
        primary("$label:")
        appendSpace()
        info(text)
    }

    private fun roleDescriptionText(role: WerwolfRoles): String = role.description.plain()

    private fun roleStartHint(role: WerwolfRoles): String = when (role) {
        WerwolfRoles.WERWOLF -> "Stimme dich nachts mit den anderen Wölfen ab und bleib tagsüber unauffällig."
        WerwolfRoles.VILLAGER -> "Beobachte das Dorf, höre gut zu und finde die Werwölfe über Diskussionen und Votes."
        WerwolfRoles.SEER -> "Nutze deine nächtliche Prüfung gezielt. Teile Wissen vorsichtig, damit du nicht sofort auffällst."
        WerwolfRoles.WITCH -> "Deine Tränke sind einmalig. Warte auf den richtigen Moment und überspringe, wenn kein Ziel sinnvoll ist."
        WerwolfRoles.AMOR -> "Wähle in der ersten Nacht zwei Spieler als Liebespaar. Ihr Schicksal hängt danach zusammen."
        WerwolfRoles.DOCTOR -> "Schütze jede Nacht einen lebenden Spieler. Gute Reads können das Dorf retten."
        WerwolfRoles.GIRL -> "Du kannst nachts riskant schauen. Je öfter du dich zeigst, desto gefährlicher wird es."
        WerwolfRoles.MAYOR -> "Deine Stimme zählt stärker. Nutze sie sichtbar, aber nicht leichtfertig."
        WerwolfRoles.PRIEST -> "Dein Weihwasser kann einen Werwolf sofort treffen, kostet dich aber das Leben bei einem Fehlwurf."
        WerwolfRoles.SERIAL_KILLER -> "Du spielst allein. Entferne nachts gezielt Spieler und halte beide Seiten im Gleichgewicht."
    }

    fun finishGame(winner: GameOutcome) {
        val shouldFinish = synchronized(lock) {
            if (stopRequested || _phase == GamePhase.IDLE) return@synchronized false
            stopRequested = true
            true
        }
        if (!shouldFinish) return

        messenger.announceWinner(winner)

        plugin.launch {
            if (WerewolfGameManager.isBaseSession(gameId)) {
                GameService.endGame(GameStopReason.HANDLER)
            } else {
                WerewolfGameManager.removeGame(gameId, allParticipants)
            }
        }
    }

    fun stop(): Unit = synchronized(lock) {
        werewolfTask?.cancel(CancellationException("Werewolf game '$gameId' stopped"))
        werewolfTask = null
        werewolfUiTask?.cancel(CancellationException("Werewolf game '$gameId' stopped"))
        werewolfUiTask = null
        stopRequested = false
        phaseSessionId += 1
        _phase = GamePhase.IDLE
        _werewolfTime = 0.seconds
        werewolfStartedAt = null
        clearWerewolfGlowing()
        clearWitchGlowing()
        clearBlindness()
        restoreHiddenPlayerVisibility()

        players.forEach { (uuid, werewolfPlayer) ->
            uuid.toBukkitPlayer()?.let { player ->
                restorePlayerGameMode(player, werewolfPlayer)
                player.removeFromWerewolfScoreboard()
            }
        }
        _leader?.toBukkitPlayer()?.removeFromWerewolfScoreboard()

        messenger.announceGameStopped()

        pendingNightExecutions.clear()
        players.clear()
        _leader = null

        // Cleanup Voice Chat
        audioHandler.clearPrivateChannel()
        privateWerewolfVoiceChatActive = false
        WerewolfVoicechatPlugin.removeAudioHandler(gameId)
    }

    fun removePlayer(player: Player): Boolean = removePlayer(player.uniqueId, player)

    fun removePlayer(uuid: UUID): Boolean = removePlayer(uuid, uuid.toBukkitPlayer())

    private fun removePlayer(playerId: UUID, player: Player?): Boolean = synchronized(lock) {
        val werewolfPlayer = players.remove(playerId) ?: return@synchronized false

        pendingNightExecutions.removeAll { it == playerId }
        players.values.forEach { other ->
            if (other.inLoveWith == playerId) {
                other.inLoveWith = null
            }
        }

        engine.removePlayer(playerId)
        audioHandler.removePlayer(playerId)

        if (player != null) {
            player.removePotionEffect(PotionEffectType.BLINDNESS)
            restorePlayerGameMode(player, werewolfPlayer)
            player.removeFromWerewolfScoreboard()
        }

        clearWerewolfGlowing()
        clearWitchGlowing()
        restoreVisibilityForPlayer(playerId)

        announceToAll {
            appendErrorPrefix()
            variableValue(player?.name ?: Bukkit.getOfflinePlayer(playerId).name ?: "#Unbekannt")
            appendSpace()
            if (player != null) {
                error("hat die Verbindung verloren und wurde aus dem Spiel entfernt.")
            } else {
                error("wurde aus dem Spiel entfernt.")
            }
        }

        if (_phase == GamePhase.RUNNING) {
            engine.checkWinCondition()?.let(::finishGame)
        }

        if (_phase != GamePhase.IDLE) {
            refreshCommandRequirements()
        }

        true
    }

    private fun makeGlowing(werewolf: Player) {
        val currentTargetId = engine.getWerewolfTargetFromLineOfSight(werewolf)
        val previousTargetId = glowingTargetsByWerewolf[werewolf.uniqueId]

        if (currentTargetId == previousTargetId) return

        previousTargetId?.toBukkitPlayer()?.let { previousTarget ->
            SurfGlowingApi.removeGlowing(previousTarget, werewolf)
        }

        if (currentTargetId == null) {
            glowingTargetsByWerewolf.remove(werewolf.uniqueId)
            return
        }

        val currentTarget = currentTargetId.toBukkitPlayer()
        if (currentTarget == null) {
            glowingTargetsByWerewolf.remove(werewolf.uniqueId)
            return
        }

        SurfGlowingApi.makeGlowing(currentTarget, werewolf, NamedTextColor.RED)
        glowingTargetsByWerewolf[werewolf.uniqueId] = currentTargetId
    }

    private fun makeWitchGlowing(witch: Player) {
        val currentTargetIds = engine.getWitchHealTargets(witch)
        val previousTargetIds = glowingTargetsByWitch[witch.uniqueId] ?: emptySet()

        if (currentTargetIds == previousTargetIds) return

        (previousTargetIds - currentTargetIds).forEach { previousTargetId ->
            previousTargetId.toBukkitPlayer()?.let { previousTarget ->
                SurfGlowingApi.removeGlowing(previousTarget, witch)
            }
        }

        if (currentTargetIds.isEmpty()) {
            glowingTargetsByWitch.remove(witch.uniqueId)
            return
        }

        val validTargetIds = mutableSetOf<UUID>()
        currentTargetIds.forEach { currentTargetId ->
            val currentTarget = currentTargetId.toBukkitPlayer() ?: return@forEach
            SurfGlowingApi.makeGlowing(currentTarget, witch, NamedTextColor.DARK_PURPLE)
            validTargetIds += currentTargetId
        }

        if (validTargetIds.isEmpty()) {
            glowingTargetsByWitch.remove(witch.uniqueId)
            return
        }

        glowingTargetsByWitch[witch.uniqueId] = validTargetIds
    }

    private suspend fun clearWerewolfGlowingNow() {
        val glowingTargets = synchronized(glowingTargetsByWerewolf) {
            if (glowingTargetsByWerewolf.isEmpty()) return
            glowingTargetsByWerewolf.toMap().also { glowingTargetsByWerewolf.clear() }
        }
        removeWerewolfGlowing(glowingTargets)
    }

    private suspend fun clearWitchGlowingNow() {
        val glowingTargets = synchronized(glowingTargetsByWitch) {
            if (glowingTargetsByWitch.isEmpty()) return
            glowingTargetsByWitch.toMap().also { glowingTargetsByWitch.clear() }
        }
        removeWitchGlowing(glowingTargets)
    }

    private suspend fun clearBlindnessNow(playerIds: Collection<UUID> = getAlivePlayers().map(WerewolfPlayer::uuid)) {
        playerIds.forEach { playerId ->
            val player = playerId.toBukkitPlayer() ?: return@forEach

            withContext(plugin.entityDispatcher(player)) {
                player.removePotionEffect(PotionEffectType.BLINDNESS)
            }
        }
    }

    private fun clearBlindness() {
        val alivePlayerIds = getAlivePlayers().map(WerewolfPlayer::uuid)
        if (alivePlayerIds.isEmpty()) return

        plugin.launch {
            clearBlindnessNow(alivePlayerIds)
        }
    }

    private fun restorePlayerGameMode(player: Player, werewolfPlayer: WerewolfPlayer) {
        val previousGameMode = werewolfPlayer.previousGameMode ?: return

        plugin.launch(plugin.entityDispatcher(player)) {
            if (player.gameMode != GameMode.SPECTATOR) return@launch
            player.gameMode = previousGameMode
        }
    }

    private fun clearWerewolfGlowing() {
        val glowingTargets = synchronized(glowingTargetsByWerewolf) {
            if (glowingTargetsByWerewolf.isEmpty()) return
            glowingTargetsByWerewolf.toMap().also { glowingTargetsByWerewolf.clear() }
        }

        plugin.launch {
            removeWerewolfGlowing(glowingTargets)
        }
    }

    private fun clearWitchGlowing() {
        val glowingTargets = synchronized(glowingTargetsByWitch) {
            if (glowingTargetsByWitch.isEmpty()) return
            glowingTargetsByWitch.toMap().also { glowingTargetsByWitch.clear() }
        }

        plugin.launch {
            removeWitchGlowing(glowingTargets)
        }
    }

    private suspend fun removeWerewolfGlowing(glowingTargets: Map<UUID, UUID>) {
        glowingTargets.forEach { (werewolfId, targetId) ->
            val werewolf = werewolfId.toBukkitPlayer() ?: return@forEach
            val target = targetId.toBukkitPlayer() ?: return@forEach

            withContext(plugin.entityDispatcher(werewolf)) {
                SurfGlowingApi.removeGlowing(target, werewolf)
            }
        }
    }

    private suspend fun removeWitchGlowing(glowingTargets: Map<UUID, Set<UUID>>) {
        glowingTargets.forEach { (witchId, targetIds) ->
            val witch = witchId.toBukkitPlayer() ?: return@forEach

            withContext(plugin.entityDispatcher(witch)) {
                targetIds.forEach { targetId ->
                    val target = targetId.toBukkitPlayer() ?: return@forEach
                    SurfGlowingApi.removeGlowing(target, witch)
                }
            }
        }
    }

    fun executePlayer(playerToExecute: UUID): Unit = synchronized(lock) {
        val executionChain = collectExecutionChain(playerToExecute)
        if (executionChain.isEmpty()) return@synchronized

        executionChain.forEach { executedPlayerId ->
            players[executedPlayerId]?.isAlive = false
        }

        messenger.announceLeaderEliminationChain(executionChain, _state)

        if (_state == GameState.NIGHT) {
            pendingNightExecutions.addAll(executionChain)
            return@synchronized
        }

        applyEliminations(executionChain)
    }

    private fun collectExecutionChain(
        playerToExecute: UUID,
        collectedPlayers: LinkedHashSet<UUID> = linkedSetOf(),
    ): List<UUID> {
        val player = players[playerToExecute] ?: return collectedPlayers.toList()
        if (!player.isAlive) return collectedPlayers.toList()
        if (!collectedPlayers.add(playerToExecute)) return collectedPlayers.toList()

        val loverId = player.inLoveWith
        if (loverId != null && loverId !in collectedPlayers) {
            val lover = players[loverId]
            if (lover?.isAlive == true) {
                collectExecutionChain(loverId, collectedPlayers)
            }
        }

        return collectedPlayers.toList()
    }

    fun executePendingNightExecutions(): Unit = synchronized(lock) {
        val executedPlayers = pendingNightExecutions.toList()
        pendingNightExecutions.clear()

        applyEliminations(executedPlayers)
    }

    private fun executePendingNightExecutionsBeforeGameEnd() {
        if (pendingNightExecutions.isEmpty()) return
        executePendingNightExecutions()
    }

    fun debugAdvancePhase(): PhaseAdvanceResult? = synchronized(lock) {
        if (_phase != GamePhase.RUNNING || _isPhaseTransitioning) return@synchronized null

        val advanceResult = engine.advancePhase()
        if (advanceResult.winner != null) {
            executePendingNightExecutionsBeforeGameEnd()
            finishGame(advanceResult.winner)
            return@synchronized advanceResult
        }

        when (advanceResult.nextPhase) {
            GameState.NIGHT -> {
                messenger.announcePhaseStarted(GameState.NIGHT)
                engine.announceCurrentNightStep()
            }

            GameState.DAY -> {
                messenger.announceDayStarted(pendingNightExecutions.toList())
                executePendingNightExecutions()
            }

            GameState.VOTE -> Unit
            GameState.MAYOR_VOTE -> Unit
        }

        refreshCommandRequirements()
        advanceResult
    }

    private fun applyEliminations(executedPlayers: List<UUID>) {
        if (executedPlayers.isEmpty()) return

        val currentPhaseSessionId = phaseSessionId

        executedPlayers.forEach { deadPlayerId ->
            audioHandler.removePlayer(deadPlayerId)
            restoreVisibilityForPlayer(deadPlayerId)
        }
        clearWerewolfGlowing()
        clearWitchGlowing()
        messenger.announceEliminatedRoles(executedPlayers)

        plugin.launch {
            executedPlayers.forEach { deadPlayerId ->
                if (phase != GamePhase.RUNNING || phaseSessionId != currentPhaseSessionId) return@launch

                val deadPlayer = deadPlayerId.toBukkitPlayer() ?: return@forEach

                withContext(plugin.entityDispatcher(deadPlayer)) {
                    if (phase != GamePhase.RUNNING || phaseSessionId != currentPhaseSessionId) return@withContext
                    deadPlayer.removePotionEffect(PotionEffectType.BLINDNESS)
                    deadPlayer.gameMode = GameMode.SPECTATOR
                }
            }
        }

        refreshCommandRequirements()
    }

    private fun restoreParticipantVisibility() {
        val participantIds = allParticipantIds()
        if (participantIds.size < 2) return

        plugin.launch {
            for (viewerId in participantIds) {
                val viewer = viewerId.toBukkitPlayer() ?: continue

                withContext(plugin.entityDispatcher(viewer)) {
                    for (targetId in participantIds) {
                        if (viewerId == targetId) continue

                        val target = targetId.toBukkitPlayer() ?: continue
                        viewer.showPlayer(plugin, target)
                    }
                }
            }
        }
    }

    private fun restoreHiddenPlayerVisibility() {
        val pairs = synchronized(hiddenPlayerPairs) {
            if (hiddenPlayerPairs.isEmpty()) return
            hiddenPlayerPairs.toSet().also { hiddenPlayerPairs.clear() }
        }

        WerewolfVisibilityCleanup.queueRestore(pairs)
    }

    private fun restoreVisibilityForPlayer(playerId: UUID) {
        val affectedPairs = synchronized(hiddenPlayerPairs) {
            val affected = hiddenPlayerPairs
                .filter { it.viewerId == playerId || it.targetId == playerId }
                .toSet()

            if (affected.isEmpty()) return
            hiddenPlayerPairs.removeAll(affected)
            affected
        }

        WerewolfVisibilityCleanup.queueRestore(affectedPairs)
    }

    private fun allParticipantIds(): Set<UUID> = buildSet {
        addAll(players.keys)
        _leader?.let(::add)
    }

    fun getAlivePlayers(): List<WerewolfPlayer> = players.values.toList().filter { it.isAlive }

    fun getDeadPlayers(): List<WerewolfPlayer> = players.values.toList().filterNot { it.isAlive }

    fun isLeader(uuid: UUID): Boolean = leader == uuid

    val allParticipants: List<Player>
        get() {
            val result = mutableListOf<Player>()

            players.keys.toList().forEach { uuid ->
                uuid.toBukkitPlayer()?.let { result.add(it) }
            }

            leader?.toBukkitPlayer()?.let { if (!result.contains(it)) result.add(it) }
            return result
        }

    fun getAllPlayers(): List<WerewolfPlayer> = players.values.toList()

    fun announceToAll(content: SurfComponentBuilder.() -> Unit) = messenger.announceToAll(content)

    fun announceToLeader(content: SurfComponentBuilder.() -> Unit) = messenger.announceToLeader(content)

    fun announceToRole(role: WerwolfRoles, onlyAlive: Boolean = true, content: SurfComponentBuilder.() -> Unit) = messenger.announceToRole(role, onlyAlive, content)

    fun announceToAlive(content: SurfComponentBuilder.() -> Unit) = messenger.announceToAlive(content)

    fun refreshCommandRequirements() = WerewolfCommandRequirements.update(allParticipants)

    fun getPlayerRole(uuid: UUID): WerwolfRoles? = players[uuid]?.role

    fun setGameState(gameState: GameState): Unit = synchronized(lock) {
        _state = gameState
    }

    fun syncWerewolfPrivateChannel(nightStep: NightStep?): Unit = synchronized(lock) {
        if (nightStep == null) {
            removePlayersFromPrivateChannel()
            return@synchronized
        }

        if (nightStep != NightStep.WEREWOLVES) {
            mutePlayersAtNight()
            return@synchronized
        }

        val aliveWerewolves = players.values
            .asSequence()
            .filter { it.isAlive && it.role == WerwolfRoles.WERWOLF }
            .mapNotNull { it.uuid.toBukkitPlayer() }
            .toList()

        if (aliveWerewolves.isEmpty()) {
            removePlayersFromPrivateChannel()
            return@synchronized
        }

        movePlayersToPrivateChannel(aliveWerewolves)
    }

    fun movePlayersToPrivateChannel(playerList: List<Player>): Unit = synchronized(lock) {
        if (playerList.isEmpty()) return@synchronized

        val silencedPlayers = players.values
            .asSequence()
            .filter { it.isAlive && it.role != WerwolfRoles.WERWOLF && it.uuid != _leader }
            .mapNotNull { it.uuid.toBukkitPlayer() }
            .toList()

        val api = WerewolfVoicechatPlugin.getVoicechatApi()
        audioHandler.configurePrivateChannel(playerList, silencedPlayers, api)
        if (privateWerewolfVoiceChatActive) return@synchronized

        privateWerewolfVoiceChatActive = true

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendSuccessPrefix()
            success("Ihr könnt jetzt untereinander sprechen!")
        }

        messenger.announceLeaderVoiceChatOpened(playerList.map(Player::getUniqueId))
    }

    private fun mutePlayersAtNight(): Unit = synchronized(lock) {
        val silencedPlayers = players.values
            .asSequence()
            .filter { it.isAlive && it.uuid != _leader }
            .mapNotNull { it.uuid.toBukkitPlayer() }
            .toList()

        val api = WerewolfVoicechatPlugin.getVoicechatApi()
        audioHandler.configurePrivateChannel(emptyList(), silencedPlayers, api)

        if (!privateWerewolfVoiceChatActive) return@synchronized

        privateWerewolfVoiceChatActive = false

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendErrorPrefix()
            error("Der private Voice-Chat wurde beendet.")
        }

        messenger.announceLeaderVoiceChatClosed()
    }

    fun removePlayersFromPrivateChannel(): Unit = synchronized(lock) {
        audioHandler.clearPrivateChannel()
        if (!privateWerewolfVoiceChatActive) return@synchronized

        privateWerewolfVoiceChatActive = false

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendErrorPrefix()
            error("Der private Voice-Chat wurde beendet.")
        }

        messenger.announceLeaderVoiceChatClosed()
    }

    private suspend fun waitForPhaseTransition() {
        synchronized(lock) { _isPhaseTransitioning = true }
        refreshCommandRequirements()
        try {
            delay(phaseTransitionDelay)
        } finally {
            synchronized(lock) { _isPhaseTransitioning = false }
        }
    }
}