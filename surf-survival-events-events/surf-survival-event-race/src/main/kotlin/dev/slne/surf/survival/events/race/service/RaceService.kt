package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.title
import dev.slne.surf.survival.events.base.game.PlayerRemoveReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.config.RaceConfig
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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object RaceService {
    private val racePlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val spectators = ConcurrentHashMap.newKeySet<UUID>()
    private val playerNautilus = ConcurrentHashMap<UUID, UUID>()

    @Volatile
    private var raceState = RaceState.DEACTIVATED

    @Volatile
    private var countdown = 10

    @Volatile
    private var task: ScheduledTask? = null

    fun addPlayer(uuid: UUID) {
        racePlayers.add(uuid)
        teleportToRaceLobby(uuid)
    }

    fun removePlayer(uuid: UUID) {
        removeNautilus(uuid)
        ProgressService.removePlayer(uuid)
        racePlayers.remove(uuid)
    }

    fun removePlayer(player: Player, teleportToServerLobby: Boolean = true) {
        val uuid = player.uniqueId

        removePlayer(uuid)

        GameService.remove(uuid, includeSpectators = true, reason = PlayerRemoveReason.KICK)
        if (teleportToServerLobby) {
            teleportToRaceLobby(uuid)
        }
    }

    fun eliminatePlayer(player: Player) {
        val uuid = player.uniqueId

        removeNautilus(uuid)
        ProgressService.removePlayer(uuid)
        racePlayers.remove(uuid)
        spectators.add(uuid)

        teleportToSpectatorLocation(uuid)
    }

    fun isInRace(player: Player) = racePlayers.contains(player.uniqueId)

    fun getRacePlayers(): Set<UUID> = racePlayers.toSet()

    fun addSpectators(uuid: UUID) {
        spectators.add(uuid)
        teleportToSpectatorLocation(uuid)
    }

    fun getSpectatorPlayers(): Set<UUID> = spectators.toSet()

    fun removeSpectator(uuid: UUID, teleportToServerLobby: Boolean = true) {
        spectators.remove(uuid)

        GameService.remove(uuid, includeSpectators = true, reason = PlayerRemoveReason.KICK)
        if (teleportToServerLobby) {
            teleportToRaceLobby(uuid)
        }
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
                    plugin.launch {
                        RegionService.fillBlocks(Material.AIR)
                    }
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
                eliminatePlayer(player)
            } ?: run {
                removePlayer(uuid)
            }
        }

        survivors.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.let { player ->
                ProgressService.resetProgress(uuid)

                player.sendText {
                    appendInfoPrefix()
                    info("Du hast es in die nächste Runde geschafft!")
                }

                removeNautilus(uuid)
            }
        }

        playerToStartMid()
    }

    fun playerToStartMid() {
        setRaceState(RaceState.WAITING)

        val starts = RaceConfig.getConfig().starts
        if (starts.isEmpty()) return

        racePlayers
            .mapNotNull { Bukkit.getPlayer(it) }
            .forEachIndexed { index, player ->
                val start = starts.getOrNull(index) ?: starts.first()
                val location = midStartLocation(start)

                plugin.launch {
                    withContext(plugin.entityDispatcher(player)) {
                        player.teleportAsync(location).await()
                        setPlayerOnNautilus(player)
                    }
                }
            }
    }

    suspend fun stopRace(notifyPlayers: Boolean = true) {
        task?.cancel()
        task = null
        setRaceState(RaceState.DEACTIVATED)
        ProgressService.clear()
        RegionService.fillBlocks(Material.AIR)

        getRacePlayers().forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                removePlayer(player)
                if (notifyPlayers) {
                    player.sendText {
                        appendInfoPrefix()
                        info("Das Rennen wurde gestoppt.")
                    }
                }
            } else {
                removePlayer(uuid)
            }
        }

        getSpectatorPlayers().forEach { uuid ->
            removeSpectator(uuid, teleportToServerLobby = true)
            if (notifyPlayers) {
                Bukkit.getPlayer(uuid)?.sendText {
                    appendInfoPrefix()
                    info("Das Rennen wurde gestoppt.")
                }
            }
        }

        playerNautilus.clear()
    }

    private fun teleportToRaceLobby(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        player.teleportAsync(RaceConfig.getConfig().roundLobbyLocation)
    }

    private fun teleportToSpectatorLocation(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        plugin.launch {
            player.teleportAsync(RaceConfig.getConfig().spectatorLocation)
        }
    }

    private fun removeNautilus(uuid: UUID) {
        playerNautilus.remove(uuid)?.let { nautilusId ->
            Bukkit.getEntity(nautilusId)?.remove()
        }
    }

    private fun midStartLocation(start: RaceConfig.StartConfig): Location {
        val world = Bukkit.getWorld(start.world)
            ?: error("Configured race start world '${start.world}' is not loaded")

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
