package dev.slne.surf.survival.events.red.light.green.light.visual

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.survival.events.red.light.green.light.config.RlglConfig
import dev.slne.surf.survival.events.red.light.green.light.plugin
import dev.slne.surf.survival.events.red.light.green.light.service.RlglState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

object RlglPlayerFeedback {

    private const val TICK_MILLIS = 500L
    private const val HEARTBEAT_EVERY_TICKS = 2

    @Volatile
    private var job: Job? = null

    fun start(recipients: () -> Collection<Player>, state: () -> RlglState) {
        stop()

        job = plugin.launch {
            var tick = 0

            while (isActive) {
                val currentState = state()
                val finish = RlglConfig.getConfig().finish
                val players = recipients()

                players.forEach { player ->
                    player.sendActionBar(buildText {
                        if (currentState == RlglState.RED) error("ROT") else success("GRÜN")
                        appendSpace()
                        info("•")
                        appendSpace()
                        info("Noch")
                        appendSpace()
                        variableValue(distanceToBox(player.location, finish).toInt().toString())
                        appendSpace()
                        info("Blöcke bis zum Ziel")
                    })
                }

                if (currentState == RlglState.RED && tick % HEARTBEAT_EVERY_TICKS == 0) {
                    players.forEach { player ->
                        player.playSound(true) {
                            type(Sound.ENTITY_WARDEN_HEARTBEAT)
                            volume(1f)
                            pitch(1f)
                        }
                    }
                }

                tick++
                delay(TICK_MILLIS.milliseconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun distanceToBox(location: Location, box: BoundingBox): Double {
        val dx = when {
            location.x < box.minX -> box.minX - location.x
            location.x > box.maxX + 1.0 -> location.x - (box.maxX + 1.0)
            else -> 0.0
        }
        val dz = when {
            location.z < box.minZ -> box.minZ - location.z
            location.z > box.maxZ + 1.0 -> location.z - (box.maxZ + 1.0)
            else -> 0.0
        }
        return sqrt(dx * dx + dz * dz)
    }
}