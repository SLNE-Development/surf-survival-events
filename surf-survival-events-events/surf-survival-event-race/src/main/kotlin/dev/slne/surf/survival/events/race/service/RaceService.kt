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
        val uuidNautilus = playerNautilus[player.uniqueId] ?: return false

        Bukkit.getEntity(uuidNautilus)?.remove()
        playerNautilus.remove(player.uniqueId)

        ProgressService.removePlayer(uuid)

        plugin.launch {
            player.teleportAsync(centralSpawn).await()
        }



        return racePlayers.remove(uuid)
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
        countdown = 10

        if (task != null) return
        task = Bukkit.getAsyncScheduler().runAtFixedRate(
            plugin,
            { scheduledTask ->

                if (countdown <= 0) {

                    setRaceState(RaceState.RUNNING)
                    RegionService.fillBlocks(Material.AIR)
                    getRacePlayers().forEach { uuid ->
                        ProgressService.addPlayer(uuid)
                    }

                    scheduledTask.cancel()
                    task = null

                    return@runAtFixedRate
                }

                getRacePlayers().forEach { uuid ->
                    val player = Bukkit.getPlayer(uuid) ?: return@forEach
                    showTitle(player)
                }

                getSpectatorPlayers().forEach { uuid ->
                    val player = Bukkit.getPlayer(uuid) ?: return@forEach
                    showTitle(player)
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
                            if (countdown <= 0) {
                                text("LOS!", Colors.VARIABLE_VALUE)
                            } else {
                                text(countdown.toString(), Colors.VARIABLE_VALUE)
                            }
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


    fun nextRound(int: Int) {
        val places = ProgressService.getPlaceList().toList()

        val survivors = places.take(int)
        val eliminated = places.drop(int)

        eliminated.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)

            player?.sendText {
                appendInfoPrefix()
                info("Du bist leider raus, danke fürs Mitmachen!")
            }

            ProgressService.removePlayer(uuid)
            player?.let { removePlayer(it) }
        }

        survivors.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid) ?: return@forEach

            ProgressService.setFinished(uuid, false)

            player.sendText {
                appendInfoPrefix()
                info("Du hast es in die nächste Runde geschafft!")
            }

            playerNautilus[uuid]?.let {
                Bukkit.getEntity(it)?.remove()
            }
        }

        playerToStartMid()
    }

    fun playerToStartMid() {

        setRaceState(RaceState.WAITING)

        val players = getRacePlayers()
            .mapNotNull { Bukkit.getPlayer(it) }

        val starts = SurfRaceConfig.getConfig().start

        players.forEachIndexed { index, player ->

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

        return Location(
            world,
            midX,
            midY,
            midZ,
            start.yaw,
            start.pitch
        )
    }



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

    fun getSpectatorPlayers(): MutableList<UUID> {
        return spectators
    }

    fun removeSpectator(uuid: UUID) {
        spectators.remove(uuid)
    }
}




