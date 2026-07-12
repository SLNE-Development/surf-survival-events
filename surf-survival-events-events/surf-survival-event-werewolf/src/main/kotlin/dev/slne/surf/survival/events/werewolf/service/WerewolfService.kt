package dev.slne.surf.survival.events.werewolf.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
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
import org.bukkit.GameMode
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
    private val glowingTargetsByWitch = mutableMapOf<UUID, Set<UUID>>()
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

        val player = uuid.toBukkitPlayer()
            ?: return WerewolfJoinResult.Error("Dein Spieler konnte nicht gefunden werden!")

        player.addToWerewolfScoreboard()
        players[uuid] = WerewolfPlayer(uuid, previousGameMode = player.gameMode)

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

        val minPlayers = 8
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
                        appendInfoPrefix()
                        gold("Deine Rolle", TextDecoration.BOLD)
                        appendSpace()
                        spacer("-")
                        appendSpace()
                        append(role.displayName)

                        appendNewInfoPrefixedLine()
                        primary("Aufgabe:")
                        appendSpace()
                        append(role.description)

                        appendNewInfoPrefixedLine()
                        primary("Tipp:")
                        appendSpace()
                        info(roleStartHint(role))

                        appendNewInfoPrefixedLine()
                        primary("Aktionen:")
                        appendSpace()
                        info("Wenn du am Zug bist, bekommst du klickbare Commands im Chat.")
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
                    _werewolfTime += 1.seconds

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
        clearWitchGlowing()
        clearBlindness()
        restoreHiddenPlayerVisibility()

        players.forEach { (uuid, werewolfPlayer) ->
            uuid.toBukkitPlayer()?.let { player ->
                restorePlayerGameMode(player, werewolfPlayer)
                player.removeFromWerewolfScoreboard()
            }
        }
        leader?.toBukkitPlayer()?.removeFromWerewolfScoreboard()

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
        val werewolfPlayer = players.remove(playerId) ?: return false

        pendingNightExecutions.removeAll { it == playerId }
        players.values.forEach { werewolfPlayer ->
            if (werewolfPlayer.inLoveWith == playerId) {
                werewolfPlayer.inLoveWith = null
            }
        }

        engine.removePlayer(playerId)
        audioHandler.removePlayer(playerId)

        player.removePotionEffect(PotionEffectType.BLINDNESS)
        restorePlayerGameMode(player, werewolfPlayer)
        player.removeFromWerewolfScoreboard()

        clearWerewolfGlowing()
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
        if (glowingTargetsByWerewolf.isEmpty()) return

        val glowingTargets = glowingTargetsByWerewolf.toMap()
        glowingTargetsByWerewolf.clear()
        removeWerewolfGlowing(glowingTargets)
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

    private fun restorePlayerGameMode(player: Player, werewolfPlayer: WerewolfPlayer) {
        val previousGameMode = werewolfPlayer.previousGameMode ?: return

        plugin.launch(plugin.entityDispatcher(player)) {
            if (player.gameMode != GameMode.SPECTATOR) return@launch
            player.gameMode = previousGameMode
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

    private fun executePendingNightExecutionsBeforeGameEnd() {
        if (pendingNightExecutions.isEmpty()) return
        executePendingNightExecutions()
    }

    fun debugAdvancePhase(): PhaseAdvanceResult? {
        if (phase != GamePhase.RUNNING || isPhaseTransitioning) return null

        val advanceResult = engine.advancePhase()
        if (advanceResult.winner != null) {
            executePendingNightExecutionsBeforeGameEnd()
            finishGame(advanceResult.winner)
            return advanceResult
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
        return advanceResult
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

    fun getDeadPlayers() = players.values.filterNot { it.isAlive }

    fun isLeader(uuid: UUID): Boolean = leader == uuid

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

    fun getPlayerRole(uuid: UUID): WerwolfRoles? = players[uuid]?.role

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
