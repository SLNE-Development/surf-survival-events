package dev.slne.surf.survival.events.hideandseek.service

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.config.SeekerSelection
import dev.slne.surf.survival.events.hideandseek.game.*
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcast
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcastLines
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcastSound
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcastTitle
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.clearRoles
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.onlineHiders
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.onlineSeekers
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.resetPlayer
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.setRole
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.setupSpectator
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
import dev.slne.surf.survival.events.hideandseek.util.forEachPlayerConcurrently
import dev.slne.surf.survival.events.hideandseek.util.formatClock
import dev.slne.surf.survival.events.hideandseek.util.formatLongDuration
import dev.slne.surf.survival.events.hideandseek.util.tp
import dev.slne.surf.survival.events.hideandseek.util.tpToServerLobby
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

object HideAndSeekService {

    enum class Phase { IDLE, LOBBY, PREPARATION, SEEKING, CELEBRATION }

    enum class EndReason { TIME_UP, HIDERS_WIN, SEEKERS_WIN }

    @Volatile
    var phase = Phase.IDLE
        private set

    @Volatile
    var remainingSeconds: Long = 0L
        private set

    @Volatile
    private var pendingEnd: EndReason? = null

    @Volatile
    private var gameJob: Job? = null

    val isSeekingPhase get() = phase == Phase.SEEKING

    val isRolePhase get() = phase == Phase.PREPARATION || phase == Phase.SEEKING

    private val messages get() = HideAndSeekThemes.theme.messages
    private val sounds get() = HideAndSeekThemes.theme.sounds

    context(context: GameContext)
    suspend fun startSession() {
        gameJob?.cancel("A new hide and seek session has been started.")
        gameJob = null
        clearRoles()
        HideAndSeekRoleManager.clearPendingLobby()
        pendingEnd = null
        HideAndSeekItems.resetCooldowns()
        phase = Phase.LOBBY

        val config = HideAndSeekConfig.getConfig()
        remainingSeconds = config.timers.lobbySeconds

        val lobbySpawn = config.lobbySpawn.toLocation(context)
        context.onlineParticipants.forEachPlayerConcurrently { player ->
            resetPlayer(player)
            player.tp(lobbySpawn)
            HideAndSeekScoreboard.show(player)
        }
        context.onlineSpectators.forEachPlayerConcurrently { player ->
            setupSpectator(player, context)
        }

        broadcast(messages.allInLobby)
    }

    context(context: GameContext)
    fun beginGame(): Boolean {
        if (phase != Phase.LOBBY || gameJob != null) return false

        val startContext = context
        gameJob = plugin.launch {
            try {
                runGame(startContext)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                plugin.componentLogger.error("Hide and seek game loop failed", e)
                GameService.endGame(GameStopReason.ERROR)
            }
        }
        return true
    }

    context(context: GameContext)
    suspend fun stopSession(reason: GameStopReason) {
        phase = Phase.IDLE
        pendingEnd = null
        remainingSeconds = 0L

        gameJob?.let { job ->
            job.cancel("The hide and seek session has been stopped.")
            // A loop stuck on a single player must not keep everyone else in the event world.
            withTimeoutOrNull(5.seconds) { job.join() }
                ?: plugin.componentLogger.warn("Hide and seek game loop did not stop within 5 seconds")
        }
        gameJob = null

        HideAndSeekScoreboard.hideAll()

        withContext(plugin.globalRegionDispatcher) {
            context.eventWorld.worldBorder.reset()
        }

        if (reason != GameStopReason.PLUGIN_DISABLE) {
            context.onlineEventPlayers.forEachPlayerConcurrently { player ->
                resetPlayer(player)
                player.tpToServerLobby()
            }
        }

        clearRoles()
    }

    private suspend fun runGame(context: GameContext) {
        announceGameStart()

        countdown(HideAndSeekConfig.getConfig().timers.lobbySeconds, SHORT_ANNOUNCEMENTS) { messages.lobbyCountdown }
        if (finishEarlyIfEnded()) return

        phase = Phase.PREPARATION
        setupBorder(context)
        assignRoles(context)
        broadcast(
            messages.hidingStarted,
            HideAndSeekThemes.value("time", formatLongDuration(HideAndSeekConfig.getConfig().timers.preparationSeconds))
        )
        broadcastSound(sounds.hidingStarted)

        countdown(HideAndSeekConfig.getConfig().timers.preparationSeconds, SHORT_ANNOUNCEMENTS) { messages.hidingCountdown }
        if (finishEarlyIfEnded()) return

        phase = Phase.SEEKING
        startSeeking(context)

        countdown(HideAndSeekConfig.getConfig().timers.seekSeconds, LONG_ANNOUNCEMENTS) { messages.seekingCountdown }

        finish(pendingEnd ?: EndReason.TIME_UP)
    }

    private suspend fun finishEarlyIfEnded(): Boolean {
        val reason = pendingEnd ?: return false
        finish(reason)
        return true
    }

    private suspend fun finish(reason: EndReason) {
        phase = Phase.CELEBRATION

        when (reason) {
            EndReason.TIME_UP, EndReason.HIDERS_WIN -> broadcast(messages.hidersWin)
            EndReason.SEEKERS_WIN -> broadcast(messages.seekersWin)
        }
        broadcastSound(sounds.gameEnd)

        val players = currentContext()?.onlineEventPlayers.orEmpty()

        // Ending the game sends everyone to the server lobby; the celebration then takes place
        // there so all players see it together. This runs outside the game loop, which ending the
        // game cancels.
        plugin.launch {
            GameService.endGame(GameStopReason.HANDLER)
            HideAndSeekCelebration.celebrate(players, HideAndSeekConfig.getConfig().timers.celebrationSeconds)
        }
    }

    /**
     * Counts down [seconds], showing the clock in the action bar and sending the themed message
     * from [announcement] (with a `<time>` placeholder) at the seconds in [announceAt].
     */
    private suspend fun countdown(
        seconds: Long,
        announceAt: Set<Long>,
        announcement: () -> String
    ) {
        for (remaining in seconds downTo 1) {
            remainingSeconds = remaining
            if (pendingEnd != null && phase != Phase.CELEBRATION) return

            val valueColor = HideAndSeekThemes.color(HideAndSeekThemes.theme.valueColor)
            val players = currentContext()?.onlineEventPlayers ?: emptyList()
            players.forEach { player ->
                player.sendActionBar(Component.text(formatClock(remaining), valueColor))
                if (remaining in 1..10) {
                    HideAndSeekThemes.playSound(player, sounds.countdownTick)
                }
            }

            if (remaining in announceAt) {
                val text = announcement()
                players.forEach { player ->
                    HideAndSeekThemes.send(player, text, HideAndSeekThemes.value("time", formatLongDuration(remaining)))
                }
            }

            delay(1.seconds)
        }
    }

    private suspend fun setupBorder(context: GameContext) {
        val config = HideAndSeekConfig.getConfig()

        withContext(plugin.globalRegionDispatcher) {
            context.eventWorld.worldBorder.apply {
                center = config.gameSpawn.toLocation(context)
                size = config.border.startRadius * 2.0
                damageAmount = config.border.damage
                damageBuffer = config.border.buffer
                warningDistance = 0
                warningTime = 5
            }
        }
    }

    private suspend fun assignRoles(context: GameContext) {
        val config = HideAndSeekConfig.getConfig()
        val candidates = context.onlineGamePlayers
        if (candidates.isEmpty()) {
            pendingEnd = EndReason.SEEKERS_WIN
            return
        }

        val seekerAmount = config.gameplay.seekerAmount
            .coerceAtMost((candidates.size - 1).coerceAtLeast(1))
        val seekers = selectSeekers(candidates, seekerAmount)

        val gameSpawn = config.gameSpawn.toLocation(context)
        candidates.forEachPlayerConcurrently { player ->
            if (player in seekers) {
                setRole(player, SeekerRole)
            } else {
                setRole(player, HiderRole)
                player.tp(gameSpawn)
            }
        }

        performPlayerCheck()
    }

    private fun selectSeekers(candidates: List<Player>, amount: Int): Set<Player> {
        val config = HideAndSeekConfig.getConfig()
        val selected = LinkedHashSet<Player>()

        if (config.gameplay.seekerSelection == SeekerSelection.FIXED) {
            val fixed = config.gameplay.fixedSeekers
                .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                .toSet()
            candidates.filter { it.uniqueId in fixed }
                .take(amount)
                .forEach(selected::add)
        }

        if (selected.size < amount) {
            candidates.filterNot(selected::contains)
                .shuffled()
                .take(amount - selected.size)
                .forEach(selected::add)
        }

        return selected
    }

    private suspend fun startSeeking(context: GameContext) {
        val config = HideAndSeekConfig.getConfig()

        broadcast(messages.seekersReleased)
        broadcastSound(sounds.seekersReleased)

        withContext(plugin.globalRegionDispatcher) {
            context.eventWorld.worldBorder.setSize(
                config.border.endRadius * 2.0,
                config.timers.seekSeconds
            )
        }

        val gameSpawn = config.gameSpawn.toLocation(context)
        onlineSeekers.forEachPlayerConcurrently { seeker ->
            seeker.tp(gameSpawn)
            HideAndSeekItems.refreshShrinkScale(seeker)
        }
    }

    suspend fun performPlayerCheck() {
        if (!isRolePhase) return

        if (onlineSeekers.isEmpty() && onlineHiders.isNotEmpty()) {
            val newSeeker = onlineHiders.random()
            setRole(newSeeker, SeekerRole)
            currentContext()?.let { context ->
                newSeeker.tp(HideAndSeekConfig.getConfig().gameSpawn.toLocation(context))
            }
        }

        if (onlineHiders.isEmpty()) {
            pendingEnd = EndReason.SEEKERS_WIN
        } else if (onlineSeekers.isEmpty()) {
            pendingEnd = EndReason.HIDERS_WIN
        }
    }

    private fun announceGameStart() {
        broadcastLines(messages.gameStartChat)
        broadcastTitle(messages.gameStartTitle, messages.gameStartSubtitle)
        broadcastSound(sounds.gameStart)
    }

    private val SHORT_ANNOUNCEMENTS = setOf<Long>(60, 30, 15, 10, 5, 4, 3, 2, 1)
    private val LONG_ANNOUNCEMENTS = setOf<Long>(
        3600, 1800, 900, 600, 300, 240, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1
    )
}
