package dev.slne.surf.survival.events.hideandseek.service

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.game.ParticipantRole
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.config.SeekerSelection
import dev.slne.surf.survival.events.hideandseek.game.*
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcast
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcastSound
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcastTitle
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.clearRoles
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.onlineHiders
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.onlineSeekers
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.resetPlayer
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.setRole
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager.setupSpectator
import dev.slne.surf.survival.events.hideandseek.util.formatClock
import dev.slne.surf.survival.events.hideandseek.util.formatLongDuration
import dev.slne.surf.survival.events.hideandseek.util.tp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import org.bukkit.Sound as BukkitSound

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

    context(context: GameContext)
    suspend fun startSession() {
        gameJob?.cancel("A new hide and seek session has been started.")
        gameJob = null
        clearRoles()
        pendingEnd = null
        HideAndSeekItems.resetCooldowns()
        phase = Phase.LOBBY

        val config = HideAndSeekConfig.getConfig()
        remainingSeconds = config.timers.lobbySeconds

        context.onlineParticipants.forEach { player ->
            resetPlayer(player)
            player.tp(config.lobbySpawn.toLocation(context))
            HideAndSeekScoreboard.show(player)
        }
        context.onlineSpectators.forEach { player ->
            setupSpectator(player, context)
        }

        broadcast {
            appendInfoPrefix()
            info("Alle Spieler sind in der Lobby!")
        }
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
        gameJob?.cancel("The hide and seek session has been stopped.")
        gameJob = null
        phase = Phase.IDLE
        pendingEnd = null
        remainingSeconds = 0L

        HideAndSeekScoreboard.hideAll()

        withContext(plugin.globalRegionDispatcher) {
            context.eventWorld.worldBorder.reset()
        }

        if (reason != GameStopReason.PLUGIN_DISABLE) {
            context.onlineEventPlayers.forEach { player ->
                resetPlayer(player)
                GameService.teleportToServerLobby(player)
            }
        }

        clearRoles()
    }

    private suspend fun runGame(context: GameContext) {
        announceGameStart()

        countdown(HideAndSeekConfig.getConfig().timers.lobbySeconds, SHORT_ANNOUNCEMENTS) { remaining ->
            appendInfoPrefix()
            info("Das Spiel beginnt in ")
            variableValue(formatLongDuration(remaining))
            info(".")
        }
        if (finishEarlyIfEnded()) return

        phase = Phase.PREPARATION
        setupBorder(context)
        assignRoles(context)
        broadcast {
            appendInfoPrefix()
            info("Los geht's! Die Verstecker haben ")
            variableValue(formatLongDuration(HideAndSeekConfig.getConfig().timers.preparationSeconds))
            info(" Zeit, um sich zu verstecken.")
        }
        broadcastSound(BukkitSound.ENTITY_ENDER_DRAGON_GROWL, volume = .5f, pitch = .75f)

        countdown(HideAndSeekConfig.getConfig().timers.preparationSeconds, SHORT_ANNOUNCEMENTS) { remaining ->
            appendInfoPrefix()
            info("Noch ")
            variableValue(formatLongDuration(remaining))
            info(", dann ziehen die Sucher los!")
        }
        if (finishEarlyIfEnded()) return

        phase = Phase.SEEKING
        startSeeking(context)

        countdown(HideAndSeekConfig.getConfig().timers.seekSeconds, LONG_ANNOUNCEMENTS) { remaining ->
            appendInfoPrefix()
            info("Noch ")
            variableValue(formatLongDuration(remaining))
            info(" bis zum Spielende.")
        }

        finish(pendingEnd ?: EndReason.TIME_UP)
    }

    private suspend fun finishEarlyIfEnded(): Boolean {
        val reason = pendingEnd ?: return false
        finish(reason)
        return true
    }

    private suspend fun finish(reason: EndReason) {
        phase = Phase.CELEBRATION

        broadcast {
            appendInfoPrefix()
            info("Spielende! ")
            when (reason) {
                EndReason.TIME_UP, EndReason.HIDERS_WIN ->
                    text("Die Verstecker haben gewonnen!", HiderRole.color, TextDecoration.BOLD)

                EndReason.SEEKERS_WIN ->
                    text("Die Sucher haben gewonnen!", SeekerRole.color, TextDecoration.BOLD)
            }
        }
        broadcastSound(BukkitSound.ENTITY_ENDER_DRAGON_DEATH, volume = .75f, pitch = .75f)

        moveRemainingPlayersToSpectator()

        countdown(HideAndSeekConfig.getConfig().timers.celebrationSeconds, emptySet()) { }

        plugin.launch {
            GameService.endGame(GameStopReason.HANDLER)
        }
    }

    private suspend fun moveRemainingPlayersToSpectator() {
        val context = currentContext() ?: return
        val spectatorSpawn = HideAndSeekConfig.getConfig().spectatorSpawn.toLocation(context)

        context.onlineGamePlayers.forEach { player ->
            setRole(player, SpectatorRole, announce = false)
            GameService.setParticipantRole(player.uniqueId, ParticipantRole.SPECTATOR)
            player.tp(spectatorSpawn)
        }
    }

    private suspend fun countdown(
        seconds: Long,
        announceAt: Set<Long>,
        announce: SurfComponentBuilder.(remaining: Long) -> Unit
    ) {
        for (remaining in seconds downTo 1) {
            remainingSeconds = remaining
            if (pendingEnd != null && phase != Phase.CELEBRATION) return

            val players = currentContext()?.onlineEventPlayers ?: emptyList()
            players.forEach { player ->
                player.sendActionBar(Component.text(formatClock(remaining), Colors.VARIABLE_VALUE))
                if (remaining in 1..10) {
                    player.playSound {
                        type(BukkitSound.BLOCK_NOTE_BLOCK_PLING)
                        pitch(2f)
                        volume(.5f)
                        source(Sound.Source.BLOCK)
                    }
                }
            }

            if (remaining in announceAt) {
                players.forEach { player ->
                    player.sendText { announce(remaining) }
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

        candidates.forEach { player ->
            if (player in seekers) {
                setRole(player, SeekerRole)
            } else {
                setRole(player, HiderRole)
                player.tp(config.gameSpawn.toLocation(context))
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

        broadcast {
            appendInfoPrefix()
            info("Die Vorbereitungszeit ist vorbei – die Sucher ziehen los!")
        }
        broadcastSound(BukkitSound.ENTITY_ENDER_DRAGON_GROWL, volume = .75f, pitch = .75f)

        withContext(plugin.globalRegionDispatcher) {
            context.eventWorld.worldBorder.setSize(
                config.border.endRadius * 2.0,
                config.timers.seekSeconds
            )
        }

        onlineSeekers.forEach { seeker ->
            seeker.tp(config.gameSpawn.toLocation(context))
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
        broadcast {
            appendNewline()
            text("HIDE AND SEEK", Colors.PRIMARY, TextDecoration.BOLD)
            appendNewline(2)
            info("Macht euch bereit – gleich geht's los!")
            appendNewline()
        }
        broadcastTitle {
            title { text("HIDE AND SEEK", Colors.PRIMARY, TextDecoration.BOLD) }
            subtitle { text("Das Spiel beginnt!", Colors.VARIABLE_VALUE) }
            times {
                fadeIn(10)
                stay(50)
                fadeOut(20)
            }
        }
        broadcastSound(BukkitSound.ENTITY_ENDER_DRAGON_GROWL, volume = .8f, pitch = 1.2f)
    }

    private val SHORT_ANNOUNCEMENTS = setOf<Long>(60, 30, 15, 10, 5, 4, 3, 2, 1)
    private val LONG_ANNOUNCEMENTS = setOf<Long>(
        3600, 1800, 900, 600, 300, 240, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1
    )
}
