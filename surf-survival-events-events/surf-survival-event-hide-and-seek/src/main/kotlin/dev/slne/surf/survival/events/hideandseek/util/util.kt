package dev.slne.surf.survival.events.hideandseek.util

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.kyori.adventure.sound.Sound
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.bukkit.Sound as BukkitSound

private const val TELEPORT_ATTEMPTS = 3
private val TELEPORT_ATTEMPT_TIMEOUT = 10.seconds
private val TELEPORT_RETRY_DELAY = 1.seconds

/**
 * Runs [block] on the thread that owns this entity and returns its result, or `null` if the entity
 * was removed (for example because the player disconnected) before the task could run.
 *
 * Unlike `withContext(entityDispatcher)`, this never suspends forever for a removed entity and can
 * be cancelled by a timeout.
 */
suspend fun <T> Entity.runOnEntityThread(block: () -> T): T? = suspendCancellableCoroutine { continuation ->
    val task = scheduler.run(
        plugin,
        { continuation.resumeWith(runCatching(block)) },
        { continuation.resume(null) }
    )

    if (task == null) {
        continuation.resume(null)
    }
}

/**
 * Teleports this player to [location]. Dismounts the player first and retries a few times if the
 * server refuses or stalls the teleport. Returns whether the player arrived.
 */
suspend fun Player.tp(location: Location, playSound: Boolean = true): Boolean {
    return teleportReliably("$location", playSound) { teleportAsync(location).await() }
}

/**
 * Sends this player back to the event server lobby, with the same dismount and retry handling as
 * [tp]. Returns whether the player arrived.
 */
suspend fun Player.tpToServerLobby(): Boolean {
    return teleportReliably("the server lobby", playSound = false) { GameService.teleportToServerLobby(this) }
}

private suspend fun Player.teleportReliably(
    target: String,
    playSound: Boolean,
    teleport: suspend Player.() -> Boolean
): Boolean {
    for (attempt in 1..TELEPORT_ATTEMPTS) {
        if (!isOnline) return false

        val arrived = try {
            withTimeoutOrNull(TELEPORT_ATTEMPT_TIMEOUT) {
                runOnEntityThread {
                    if (isInsideVehicle) leaveVehicle()
                    eject()
                }
                teleport()
            } ?: false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            plugin.componentLogger.warn("Teleport attempt $attempt of $name to $target failed", e)
            false
        }

        if (arrived) {
            if (playSound) {
                playSound {
                    type(BukkitSound.ENTITY_ENDERMAN_TELEPORT)
                    volume(.5f)
                    source(Sound.Source.HOSTILE)
                }
            }
            return true
        }

        if (attempt < TELEPORT_ATTEMPTS) delay(TELEPORT_RETRY_DELAY)
    }

    if (isOnline) {
        plugin.componentLogger.warn("Could not teleport $name to $target after $TELEPORT_ATTEMPTS attempts")
    }
    return false
}

/**
 * Runs [action] for every player in its own coroutine, so one player that fails or stalls never
 * holds up the others. Waits at most [timeout] for all of them to finish.
 */
suspend fun Collection<Player>.forEachPlayerConcurrently(
    timeout: Duration = 45.seconds,
    action: suspend (Player) -> Unit
) {
    val jobs = map { player ->
        plugin.launch {
            try {
                action(player)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                plugin.componentLogger.error("Hide and seek player action failed for ${player.name}", e)
            }
        }
    }

    withTimeoutOrNull(timeout) { jobs.joinAll() }
        ?: plugin.componentLogger.warn("Hide and seek player actions did not finish within $timeout")
}

fun formatClock(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun formatLongDuration(totalSeconds: Long): String {
    return if (totalSeconds >= 60 && totalSeconds % 60 == 0L) {
        val minutes = totalSeconds / 60
        if (minutes == 1L) "1 Minute" else "$minutes Minuten"
    } else {
        if (totalSeconds == 1L) "1 Sekunde" else "$totalSeconds Sekunden"
    }
}
