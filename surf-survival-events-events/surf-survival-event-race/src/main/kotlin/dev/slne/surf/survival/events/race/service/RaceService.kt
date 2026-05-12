package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.title
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Nautilus
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.TimeUnit

object RaceService {
    private val racePlayers = mutableListOf<UUID>()

    private var raceState = RaceState.DEACTIVATED

    private var countdown = 10

    private var task: ScheduledTask? = null

    fun addPlayer(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid)
        val lobby = SurfRaceConfig.getConfig().let {
            Location(
                Bukkit.getWorld(it.lobbyWorld),
                it.lobbyX,
                it.lobbyY,
                it.lobbyZ,
                it.lobbyYaw,
                it.lobbyPitch
            )
        }
        plugin.launch {
            player?.teleportAsync(lobby)
        }
        racePlayers.add(uuid)
    }

    fun isInRace(player: Player): Boolean {
        val uuid = player.uniqueId
        return racePlayers.contains(uuid)
    }

    fun getRacePlayers(): MutableList<UUID> {
        return racePlayers
    }

    fun getRaceState(): RaceState {
        return raceState
    }

    fun setRaceState(state: RaceState) {
        raceState = state
    }

    fun removePlayer(player: Player): Boolean {
        val centralSpawn = Location(player.world, 0.0, 73.0, 0.0, 0f, 0f)
        val uuid = player.uniqueId

        plugin.launch {
            player.teleportAsync(centralSpawn)
            return@launch
        }
        return racePlayers.remove(uuid)
    }

    fun setPlayerOnNautilus(player: Player) {
        plugin.launch {
            val location = player.location
            withContext(plugin.regionDispatcher(location)) {
                val nautilus = location.world.spawn(location, Nautilus::class.java) { entity ->
                    entity.inventory.addItem(ItemStack(Material.SADDLE))
                    entity.owner = player
                    entity.isInvulnerable = true
                }
                nautilus.addPassenger(player)
            }
        }
    }

    fun startCountdown() {
        if (raceState != RaceState.COUNTDOWN) return
        countdown = 10

        if (task != null) return
        task = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { scheduledTask ->

                getRacePlayers().forEach { uuid ->
                    val player = Bukkit.getPlayer(uuid) ?: return@forEach
                    showTitle(player)
                }

                if (countdown <= 0) {

                    getRacePlayers().forEach { uuid ->
                        val player = Bukkit.getPlayer(uuid) ?: return@forEach
                        setPlayerOnNautilus(player)
                    }

                    setRaceState(RaceState.RUNNING)
                    scheduledTask.cancel()
                    task = null

                    return@runAtFixedRate
                }

                countdown--
            },
            0L,
            1, TimeUnit.SECONDS
        )
    }

    private fun showTitle(player: Player) {
        plugin.launch {
            withContext(plugin.entityDispatcher(player)) {
                player.showTitle(
                    title {
                        title {
                            if (countdown == 0) {
                                text("LOS!", Colors.VARIABLE_VALUE)
                                return@withContext
                            }
                            text(countdown.toString(), Colors.VARIABLE_VALUE)
                        }
                        times {
                            fadeIn(0)
                            stay(20)
                            fadeOut(0)
                        }
                    }
                )
            }
        }
    }
}




