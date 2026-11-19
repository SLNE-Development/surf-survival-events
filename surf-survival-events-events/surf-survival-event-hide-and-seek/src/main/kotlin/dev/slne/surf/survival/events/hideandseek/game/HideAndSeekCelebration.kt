package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
import dev.slne.surf.survival.events.hideandseek.util.runOnEntityThread
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType

/** Fireworks and heart particles shown around the players while the winners are celebrated. */
object HideAndSeekCelebration : Listener {

    /** Marks celebration fireworks so they never hurt anyone. */
    private val celebrationFireworkKey = NamespacedKey(plugin, "celebration_firework")

    @EventHandler
    fun onFireworkDamage(event: EntityDamageByEntityEvent) {
        val firework = event.damager as? Firework ?: return
        if (firework.persistentDataContainer.has(celebrationFireworkKey)) {
            event.isCancelled = true
        }
    }

    /**
     * Shows the theme's celebration effects at [players] for [seconds], one burst every
     * `intervalSeconds`. Players who go offline are skipped.
     */
    suspend fun celebrate(players: Collection<Player>, seconds: Long) {
        val interval = HideAndSeekThemes.theme.celebration.intervalSeconds.coerceAtLeast(1)
        var elapsed = 0L
        while (elapsed < seconds) {
            burst(players.filter { it.isOnline })
            delay(interval.seconds)
            elapsed += interval
        }
    }

    /** Launches one burst of the theme's celebration effects at every player in [players]. */
    fun burst(players: Collection<Player>) {
        val celebration = HideAndSeekThemes.theme.celebration
        if (!celebration.fireworks && !celebration.hearts) return

        val colors = celebration.fireworkColors
            .map { Color.fromRGB(it.value()) }
            .ifEmpty { listOf(Color.WHITE) }

        players.forEach { player ->
            plugin.launch {
                // Spawning happens on the player's own region, which owns their location.
                player.runOnEntityThread {
                    val location = player.location

                    if (celebration.fireworks) {
                        location.world.spawn(location.clone().add(0.0, 1.0, 0.0), Firework::class.java) { firework ->
                            firework.persistentDataContainer.set(celebrationFireworkKey, PersistentDataType.BOOLEAN, true)
                            firework.fireworkMeta = firework.fireworkMeta.apply {
                                addEffect(
                                    FireworkEffect.builder()
                                        .with(FireworkEffect.Type.entries.random())
                                        .withColor(colors.shuffled().take(2))
                                        .withFade(Color.WHITE)
                                        .flicker(true)
                                        .build()
                                )
                                power = 1
                            }
                        }
                    }

                    if (celebration.hearts) {
                        location.world.spawnParticle(Particle.HEART, location.clone().add(0.0, 2.2, 0.0), 6, .6, .4, .6)
                    }
                }
            }
        }
    }
}
