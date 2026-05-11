package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Nautilus
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

object RaceService {
    private val racePlayers = mutableListOf<UUID>()
    private var isGameActive: Boolean = false

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

    fun setGameActive() {
        isGameActive = true
    }

    fun isGameActive(): Boolean {
        return isGameActive
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

        val location = player.location
        val nautilus = location.world.spawn(location, Nautilus::class.java) { entity ->
            entity.inventory.addItem(ItemStack(Material.SADDLE))
            entity.owner = player
            entity.isInvisible = true
        }
        nautilus.addPassenger(player)
    }
}