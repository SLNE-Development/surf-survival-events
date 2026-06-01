package dev.slne.surf.survival.events.werewolf.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.buildText
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
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.collections.plusAssign
import kotlin.time.Duration
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

    internal val players = mutableMapOf<UUID, WerewolfPlayer>()

    val leader: UUID?
        get() = _leader

    private var _leader: UUID? = null

    val phase: GamePhase
        get() = _phase

    private var _phase = GamePhase.IDLE

    val state: GameState
        get() = _state

    private var _state = GameState.DAY

    val isPhaseTransitioning: Boolean
        get() = _isPhaseTransitioning

    private var _isPhaseTransitioning = false

    private var werewolfTask: Job? = null

    val werewolfTime: Duration
        get() = _werewolfTime

    private var _werewolfTime: Duration = 0.seconds

    private val messenger = WerewolfMessenger(this)
    private var privateWerewolfVoiceChatActive = false

    val aliveCount: Int
        get() = getAlivePlayers().size

    val totalCount: Int
        get() = players.size

    private val audioHandler = WerewolfVoicechatPlugin.getAudioHandler(gameId)

    val engine: WerewolfGameEngine
        get() =_engine

    @Volatile
    private var phaseSessionId = 0

    private var _engine = WerewolfGameEngine(this)
    private val glowingTargetsByWerewolf = mutableMapOf<UUID, UUID>()
    private val glowingTargetsByDoctor = mutableMapOf<UUID, UUID>()
    private val glowingTargetsByWitch = mutableMapOf<UUID, UUID>()
    private val hiddenPlayerPairs = mutableSetOf<HiddenPlayerPair>()
    private val pendingNightExecutions = mutableListOf<UUID>()
    private val phaseTransitionDelay = 3.seconds
    private var stopRequested = false

    fun openLobby(leaderUuid: UUID?) {
        if (phase != GamePhase.IDLE) return

        players.clear()
        _phase = GamePhase.LOBBY
        _leader = leaderUuid
        stopRequested = false
    }

    fun join(uuid: UUID): WerewolfJoinResult {
        if (phase != GamePhase.LOBBY) return WerewolfJoinResult.AlreadyStarted
        if (players.containsKey(uuid)) return WerewolfJoinResult.AlreadyInGame

        uuid.toBukkitPlayer()?.addToWerewolfScoreboard() ?: return WerewolfJoinResult.Error("Dein Spieler konnte nicht gefunden werden!")

        players[uuid] = WerewolfPlayer(uuid)

        announceToAll {
            appendSuccessPrefix()
            variableValue(uuid.toBukkitPlayer()?.name ?: "#Unbekannt")
            appendSpace()
            success("ist der Werwolf-Runde beigetreten!")
        }
        return WerewolfJoinResult.Success
    }

    fun start(): WerewolfStartResult {
        if (werewolfTask != null) error("Werewolf task is already running!")

        if (phase != GamePhase.LOBBY) {
            return WerewolfStartResult.NotInLobbyPhase
        }

        // Testing
        val minPlayers = 1
        if (players.size < minPlayers) {
            val result = WerewolfStartResult.NotEnoughPlayers(players.size, minPlayers)
            return result
        }

        try {
            stopRequested = false
            phaseSessionId += 1
            _phase = GamePhase.RUNNING
            _state = GameState.DAY
            pendingNightExecutions.clear()
            restoreParticipantVisibility()
            WerewolfVisibilityCleanup.restorePendingForPlayers(allParticipantIds())
            val roleMap = WerewolfRoleSelection.assignRoles(players.keys.toList())

            this._engine.startGameEngine()

            roleMap.forEach { (uuid, role) ->
                val werewolfPlayer = players[uuid] ?: return@forEach
                werewolfPlayer.role = role

                uuid.toBukkitPlayer()?.let {
                    it.sendText {
                        // ───────────── HEADER ─────────────
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))
                        appendSpace()
                        variableValue("DEINE ROLLE", TextDecoration.BOLD)
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── ROLE NAME ─────────────
                        info("Du bist: ")
                        append(role.displayName)

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── DESCRIPTION ─────────────
                        info("Rollenbeschreibung:")
                        appendNewInfoPrefixedLine()
                        appendSpace()
                        append(role.description)

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── FLAVOR LINE ─────────────
                        if (!role.isHostile) {
                            info("Spiele deine Rolle weise… das Dorf zählt auf dich.")
                        } else {
                            info("Du bist der Schrecken der Nacht… jage klug und bleibe unentdeckt.")
                        }

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── FOOTER ─────────────
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))
                        appendSpace()
                        append(role.displayName)
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))
                    }

                    it.showDialog(
                        WerewolfRoleViewDialoge.create(it)
                    )
                }
            }

            messenger.announceLeaderRoles(roleMap)

            werewolfTask = plugin.launch {
                while (isActive) {
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

                                if (engine.currentNightStep == NightStep.DOCTOR &&
                                    werewolfPlayer.role == WerwolfRoles.DOCTOR
                                ) {
                                    makeDoctorGlowing(player)
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

                        if (engine.currentNightStep != NightStep.DOCTOR) {
                            clearDoctorGlowingNow()
                        }

                        if (engine.currentNightStep != NightStep.WITCH) {
                            clearWitchGlowingNow()
                        }
                    } else {
                        clearBlindnessNow()
                        clearWerewolfGlowingNow()
                        clearDoctorGlowingNow()
                        clearWitchGlowingNow()
                    }

                    delay(1.seconds)
                    _werewolfTime += 1.seconds

                    //Chek if Phase is over
                    val advanceResult = engine.tick()

                    if (advanceResult != null) {
                        if (advanceResult.winner != null) {
                            finishGame(advanceResult.winner)
                            return@launch
                        }

                        waitForPhaseTransition()

                        messenger.announcePhaseStarted(advanceResult.nextPhase)

                        when (advanceResult.nextPhase) {
                            GameState.NIGHT -> {
                                engine.announceCurrentNightStep()
                            }

                            GameState.DAY -> {
                                messenger.announceNightExecutionResults(pendingNightExecutions.toList())

                                executePendingNightExecutions()
                            }

                            GameState.VOTE -> Unit

                            GameState.MAYOR_VOTE -> Unit
                        }

                        refreshCommandRequirements()
                    }
                }
            }

            messenger.announceGameStarted()
            players.forEach { (uuid, _) ->
                uuid.toBukkitPlayer()?.let {
                    it.showTitle {
                        title { primary("Werwolf gestartet") }
                        subtitle { variableValue("Viel Spaß!") }
                        times {
                            fadeIn(500.milliseconds)
                            stay(3.seconds)
                            fadeOut(500.milliseconds)
                        }
                    }
                    it.playSound(true) {
                        type(Sound.BLOCK_BEACON_ACTIVATE)
                        volume(.5f)
                        pitch(.5f)
                    }
                }
            }

            refreshCommandRequirements()
            return WerewolfStartResult.Success
        } catch (e: Exception) {
            _phase = GamePhase.IDLE
            return WerewolfStartResult.Error(e.message ?: "Unbekannter Fehler beim Starten")
        }
    }

    fun finishGame(winner: GameOutcome) {
        if (stopRequested || phase == GamePhase.IDLE) return
        stopRequested = true
        messenger.announceWinner(winner)
        plugin.launch {
            GameService.endGame(GameStopReason.HANDLER)
        }
    }

    fun stop() {
        werewolfTask?.cancel(CancellationException("Werewolf game '$gameId' stopped"))
        werewolfTask = null
        stopRequested = false
        phaseSessionId += 1
        _phase = GamePhase.IDLE
        clearWerewolfGlowing()
        clearDoctorGlowing()
        clearWitchGlowing()
        clearBlindness()
        restoreHiddenPlayerVisibility()

        players.forEach { (uuid, _) ->
            uuid.toBukkitPlayer()?.removeFromWerewolfScoreboard()
        }

        messenger.announceGameStopped()

        pendingNightExecutions.clear()
        players.clear()
        _leader = null

        // Cleanup Voice Chat
        audioHandler.clearPrivateChannel()
        privateWerewolfVoiceChatActive = false
        WerewolfVoicechatPlugin.removeAudioHandler(gameId)
    }

    fun removePlayer(player: Player): Boolean {
        val playerId = player.uniqueId
        if (players.remove(playerId) == null) return false

        pendingNightExecutions.removeAll { it == playerId }
        players.values.forEach { werewolfPlayer ->
            if (werewolfPlayer.inLoveWith == playerId) {
                werewolfPlayer.inLoveWith = null
            }
        }

        engine.removePlayer(playerId)
        audioHandler.removePlayer(playerId)

        player.removePotionEffect(PotionEffectType.BLINDNESS)
        player.removeFromWerewolfScoreboard()

        clearWerewolfGlowing()
        clearDoctorGlowing()
        clearWitchGlowing()
        restoreVisibilityForPlayer(playerId)

        announceToAll {
            appendErrorPrefix()
            variableValue(player.name)
            appendSpace()
            error("hat die Verbindung verloren und wurde aus dem Spiel entfernt.")
        }

        if (phase == GamePhase.RUNNING) {
            engine.checkWinCondition()?.let(::finishGame)
        }

        if (phase != GamePhase.IDLE) {
            refreshCommandRequirements()
        }

        return true
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
        val currentTargetId = engine.getWitchHealTarget(witch)
        val previousTargetId = glowingTargetsByWitch[witch.uniqueId]

        if (currentTargetId == previousTargetId) return

        previousTargetId?.toBukkitPlayer()?.let { previousTarget ->
            SurfGlowingApi.removeGlowing(previousTarget, witch)
        }

        if (currentTargetId == null) {
            glowingTargetsByWitch.remove(witch.uniqueId)
            return
        }

        val currentTarget = currentTargetId.toBukkitPlayer()
        if (currentTarget == null) {
            glowingTargetsByWitch.remove(witch.uniqueId)
            return
        }

        SurfGlowingApi.makeGlowing(currentTarget, witch, NamedTextColor.DARK_PURPLE)
        glowingTargetsByWitch[witch.uniqueId] = currentTargetId
    }

    private fun makeDoctorGlowing(doctor: Player) {
        val currentTargetId = engine.getDoctorHealTarget(doctor)
        val previousTargetId = glowingTargetsByDoctor[doctor.uniqueId]

        if (currentTargetId == previousTargetId) return

        previousTargetId?.toBukkitPlayer()?.let { previousTarget ->
            SurfGlowingApi.removeGlowing(previousTarget, doctor)
        }

        if (currentTargetId == null) {
            glowingTargetsByDoctor.remove(doctor.uniqueId)
            return
        }

        val currentTarget = currentTargetId.toBukkitPlayer()
        if (currentTarget == null) {
            glowingTargetsByDoctor.remove(doctor.uniqueId)
            return
        }

        SurfGlowingApi.makeGlowing(currentTarget, doctor, NamedTextColor.AQUA)
        glowingTargetsByDoctor[doctor.uniqueId] = currentTargetId
    }

    private suspend fun clearWerewolfGlowingNow() {
        if (glowingTargetsByWerewolf.isEmpty()) return

        val glowingTargets = glowingTargetsByWerewolf.toMap()
        glowingTargetsByWerewolf.clear()
        removeWerewolfGlowing(glowingTargets)
    }

    private suspend fun clearDoctorGlowingNow() {
        if (glowingTargetsByDoctor.isEmpty()) return

        val glowingTargets = glowingTargetsByDoctor.toMap()
        glowingTargetsByDoctor.clear()
        removeDoctorGlowing(glowingTargets)
    }

    private suspend fun clearWitchGlowingNow() {
        if (glowingTargetsByWitch.isEmpty()) return

        val glowingTargets = glowingTargetsByWitch.toMap()
        glowingTargetsByWitch.clear()
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

    private fun clearWerewolfGlowing() {
        if (glowingTargetsByWerewolf.isEmpty()) return

        val glowingTargets = glowingTargetsByWerewolf.toMap()
        glowingTargetsByWerewolf.clear()

        plugin.launch {
            removeWerewolfGlowing(glowingTargets)
        }
    }

    private fun clearDoctorGlowing() {
        if (glowingTargetsByDoctor.isEmpty()) return

        val glowingTargets = glowingTargetsByDoctor.toMap()
        glowingTargetsByDoctor.clear()

        plugin.launch {
            removeDoctorGlowing(glowingTargets)
        }
    }

    private fun clearWitchGlowing() {
        if (glowingTargetsByWitch.isEmpty()) return

        val glowingTargets = glowingTargetsByWitch.toMap()
        glowingTargetsByWitch.clear()

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

    private suspend fun removeDoctorGlowing(glowingTargets: Map<UUID, UUID>) {
        glowingTargets.forEach { (doctorId, targetId) ->
            val doctor = doctorId.toBukkitPlayer() ?: return@forEach
            val target = targetId.toBukkitPlayer() ?: return@forEach

            withContext(plugin.entityDispatcher(doctor)) {
                SurfGlowingApi.removeGlowing(target, doctor)
            }
        }
    }

    private suspend fun removeWitchGlowing(glowingTargets: Map<UUID, UUID>) {
        glowingTargets.forEach { (witchId, targetId) ->
            val witch = witchId.toBukkitPlayer() ?: return@forEach
            val target = targetId.toBukkitPlayer() ?: return@forEach

            withContext(plugin.entityDispatcher(witch)) {
                SurfGlowingApi.removeGlowing(target, witch)
            }
        }
    }

    fun executePlayer(playerToExecute: UUID) {
        val executionChain = collectExecutionChain(playerToExecute)
        if (executionChain.isEmpty()) return

        executionChain.forEach { executedPlayerId ->
            players[executedPlayerId]?.isAlive = false
        }

        messenger.announceLeaderEliminationChain(executionChain, state)

        if (state == GameState.NIGHT) {
            pendingNightExecutions.addAll(executionChain)
            return
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

    fun executePendingNightExecutions() {
        val executedPlayers = pendingNightExecutions.toList()
        pendingNightExecutions.clear()

        applyEliminations(executedPlayers)
    }

    private fun applyEliminations(executedPlayers: List<UUID>) {
        if (executedPlayers.isEmpty()) return

        val currentPhaseSessionId = phaseSessionId
        val viewerIds = allParticipants
            .map { it.uniqueId }
            .distinct()

        plugin.launch {
            executedPlayers.forEach { deadPlayerId ->
                if (phase != GamePhase.RUNNING || phaseSessionId != currentPhaseSessionId) return@launch

                viewerIds.forEach { viewerId ->
                    if (phase != GamePhase.RUNNING || phaseSessionId != currentPhaseSessionId) return@launch
                    if (viewerId == deadPlayerId) return@forEach

                    val viewer = viewerId.toBukkitPlayer() ?: return@forEach

                    withContext(plugin.entityDispatcher(viewer)) {
                        if (phase != GamePhase.RUNNING || phaseSessionId != currentPhaseSessionId) return@withContext
                        val deadPlayer = deadPlayerId.toBukkitPlayer() ?: return@withContext
                        hiddenPlayerPairs += HiddenPlayerPair(viewerId, deadPlayerId)
                        viewer.hidePlayer(plugin, deadPlayer)
                    }
                }

                if (deadPlayerId.toBukkitPlayer()?.isVisibleByDefault == false) {
                    plugin.logger.fine("Player not visible! \n PlayerID: $deadPlayerId \n PlayerName: ${deadPlayerId.toBukkitPlayer()?.name}")
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
        if (hiddenPlayerPairs.isEmpty()) return

        WerewolfVisibilityCleanup.queueRestore(hiddenPlayerPairs.toSet())
        hiddenPlayerPairs.clear()
    }

    private fun restoreVisibilityForPlayer(playerId: UUID) {
        val affectedPairs = hiddenPlayerPairs
            .filter { it.viewerId == playerId || it.targetId == playerId }
            .toSet()

        if (affectedPairs.isEmpty()) return

        hiddenPlayerPairs.removeAll(affectedPairs)
        WerewolfVisibilityCleanup.queueRestore(affectedPairs)
    }

    private fun allParticipantIds(): Set<UUID> = buildSet {
        addAll(players.keys)
        leader?.let(::add)
    }

    fun getAlivePlayers() = players.values.filter { it.isAlive }

    val allParticipants: List<Player>
        get() {
            val result = mutableListOf<Player>()

            players.keys.forEach { uuid ->
                uuid.toBukkitPlayer()?.let { result.add(it) }
            }

            leader?.toBukkitPlayer()?.let { if (!result.contains(it)) result.add(it) }
            return result
        }

    fun getAllPlayers() = players.values

    fun announceToAll(content: SurfComponentBuilder.() -> Unit) = messenger.announceToAll(content)

    fun announceToLeader(content: SurfComponentBuilder.() -> Unit) = messenger.announceToLeader(content)

    fun announceToRole(role: WerwolfRoles, onlyAlive: Boolean = true, content: SurfComponentBuilder.() -> Unit) = messenger.announceToRole(role, onlyAlive, content)

    fun announceToAlive(content: SurfComponentBuilder.() -> Unit) = messenger.announceToAlive(content)

    fun refreshCommandRequirements() = WerewolfCommandRequirements.update(allParticipants)

    fun getPlayerRole(uuid: UUID): WerwolfRoles? {
        return players[uuid]?.role
    }

    fun setGameState(gameState: GameState) {
        _state = gameState
    }

    fun syncWerewolfPrivateChannel(nightStep: NightStep?) {
        if (nightStep == null) {
            removePlayersFromPrivateChannel()
            return
        }

        if (nightStep != NightStep.WEREWOLVES) {
            mutePlayersAtNight()
            return
        }

        val aliveWerewolves = players.values
            .asSequence()
            .filter { it.isAlive && it.role == WerwolfRoles.WERWOLF }
            .mapNotNull { it.uuid.toBukkitPlayer() }
            .toList()

        if (aliveWerewolves.isEmpty()) {
            removePlayersFromPrivateChannel()
            return
        }

        movePlayersToPrivateChannel(aliveWerewolves)
    }

    fun movePlayersToPrivateChannel(playerList: List<Player>) {
        if (playerList.isEmpty()) return

        val silencedPlayers = players.values
            .asSequence()
            .filter { it.isAlive && it.role != WerwolfRoles.WERWOLF && it.uuid != leader }
            .mapNotNull { it.uuid.toBukkitPlayer() }
            .toList()

        val api = WerewolfVoicechatPlugin.getVoicechatApi()
        audioHandler.configurePrivateChannel(playerList, silencedPlayers, api)
        if (privateWerewolfVoiceChatActive) return

        privateWerewolfVoiceChatActive = true

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendSuccessPrefix()
            success("Ihr könnt jetzt untereinander sprechen!")
        }

        messenger.announceLeaderVoiceChatOpened(playerList.map(Player::getUniqueId))
    }

    private fun mutePlayersAtNight() {
        val silencedPlayers = players.values
            .asSequence()
            .filter { it.isAlive && it.uuid != leader }
            .mapNotNull { it.uuid.toBukkitPlayer() }
            .toList()

        val api = WerewolfVoicechatPlugin.getVoicechatApi()
        audioHandler.configurePrivateChannel(emptyList(), silencedPlayers, api)

        if (!privateWerewolfVoiceChatActive) return

        privateWerewolfVoiceChatActive = false

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendErrorPrefix()
            error("Der private Voice-Chat wurde beendet.")
        }

        messenger.announceLeaderVoiceChatClosed()
    }

    fun removePlayersFromPrivateChannel() {
        audioHandler.clearPrivateChannel()
        if (!privateWerewolfVoiceChatActive) return

        privateWerewolfVoiceChatActive = false

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendErrorPrefix()
            error("Der private Voice-Chat wurde beendet.")
        }

        messenger.announceLeaderVoiceChatClosed()
    }

    private suspend fun waitForPhaseTransition() {
        _isPhaseTransitioning = true
        refreshCommandRequirements()
        try {
            delay(phaseTransitionDelay)
        } finally {
            _isPhaseTransitioning = false
        }
    }
}
