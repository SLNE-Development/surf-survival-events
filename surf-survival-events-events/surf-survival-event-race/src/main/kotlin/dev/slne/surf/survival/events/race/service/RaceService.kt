package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.title
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.coroutines.future.await
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
    private val spectators = mutableListOf<UUID>()
    private val playerNautilus = mutableMapOf<UUID, UUID>()

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

    fun removePlayer(player: Player): Boolean {
        val uuid = player.uniqueId
        val centralSpawn = Location(player.world, 0.0, 73.0, 0.0, 0f, 0f)

        val nautilusId = playerNautilus[uuid] ?: return false

        Bukkit.getEntity(nautilusId)?.remove()
        playerNautilus.remove(uuid)

        ProgressService.removePlayer(uuid)

        plugin.launch {
            player.teleportAsync(centralSpawn).await()
        }

        return racePlayers.remove(uuid)
    }

    fun isInRace(player: Player) = racePlayers.contains(player.uniqueId)

    fun getRacePlayers(): MutableList<UUID> = racePlayers

    fun addSpectators(uuid: UUID) {
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

        spectators.add(uuid)
    }

    fun getSpectatorPlayers(): MutableList<UUID> = spectators

    fun removeSpectator(uuid: UUID) {
        spectators.remove(uuid)
    }

    fun getRaceState() = raceState

    fun setRaceState(state: RaceState) {
        raceState = state
    }

    fun setPlayerOnNautilus(player: Player) {
        val location = player.location

        val nautilus = location.world.spawn(location, Nautilus::class.java) { entity ->
            entity.inventory.addItem(ItemStack(Material.SADDLE))
            entity.owner = player
            entity.isInvulnerable = true
        }

        playerNautilus[player.uniqueId] = nautilus.uniqueId
        nautilus.addPassenger(player)
    }

    fun startCountdown() {
        if (raceState != RaceState.COUNTDOWN) return
        if (task != null) return

        countdown = 10

        task = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { scheduledTask ->

                if (countdown <= 0) {

                    setRaceState(RaceState.RUNNING)
                    RegionService.fillBlocks(Material.AIR)

                    racePlayers.forEach { ProgressService.addPlayer(it) }

                    scheduledTask.cancel()
                    task = null
                    return@runAtFixedRate
                }

                racePlayers.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { showTitle(it) }
                }

                spectators.forEach { uuid ->
                    Bukkit.getPlayer(uuid)?.let { showTitle(it) }
                }

                countdown--
            },
            0L,
            1,
            TimeUnit.SECONDS
        )
    }

    fun nextRound(int: Int) {
        val places = ProgressService.getPlaceList().toList()

        val survivors = places.take(int)
        val eliminated = places.drop(int)

        eliminated.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                player.sendText {
                    appendInfoPrefix()
                    info("Du bist leider raus, danke fürs Mitmachen!")
                }
                removePlayer(player)
            }

            ProgressService.removePlayer(uuid)
        }

        survivors.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                ProgressService.setFinished(uuid, false)

                player.sendText {
                    appendInfoPrefix()
                    info("Du hast es in die nächste Runde geschafft!")
                }

                playerNautilus[uuid]?.let {
                    Bukkit.getEntity(it)?.remove()
                }
            }
        }

        playerToStartMid()
    }

    fun playerToStartMid() {
        setRaceState(RaceState.WAITING)

        val starts = SurfRaceConfig.getConfig().start

        racePlayers
            .mapNotNull { Bukkit.getPlayer(it) }
            .forEachIndexed { index, player ->

                val start = starts.getOrNull(index) ?: starts.first()
                val location = midStartLocation(start)

                plugin.launch {
                    withContext(plugin.entityDispatcher(player)) {
                        player.teleportAsync(location)
                        setPlayerOnNautilus(player)
                    }
                }
            }
    }

    private fun midStartLocation(start: SurfRaceConfig.Start): Location {
        val world = Bukkit.getWorld(start.world)

        val midX = (start.x1 + start.x2) / 2
        val midY = (start.y1 + start.y2) / 2
        val midZ = (start.z1 + start.z2) / 2

        return Location(world, midX, midY, midZ, start.yaw, start.pitch)
    }

    private fun showTitle(player: Player) {
        plugin.launch {
            withContext(plugin.entityDispatcher(player)) {
                player.showTitle(
                    title {
                        title {
                            text(
                                if (countdown <= 0) "LOS!" else countdown.toString(),
                                Colors.VARIABLE_VALUE
                            )
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




