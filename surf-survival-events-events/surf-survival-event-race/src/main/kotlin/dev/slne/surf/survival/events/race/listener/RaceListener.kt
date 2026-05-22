package dev.slne.surf.survival.events.race.listener

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.survival.events.race.service.ProgressService
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import java.util.UUID
import java.util.concurrent.TimeUnit

object RaceListener : Listener {

    private val messageCooldown = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.SECONDS)
        .build<UUID, Boolean>()

    private fun canSendMessage(uuid: UUID): Boolean {
        if (messageCooldown.getIfPresent(uuid) != null) return false
        messageCooldown.put(uuid, true)
        return true
    }

    @EventHandler
    fun onVehicleExit(event: VehicleExitEvent) {
        val player = event.exited
        if (player !is Player) return
        if (event.vehicle.type != EntityType.NAUTILUS) return
        if (!RaceService.isInRace(player)) return

        if (canSendMessage(player.uniqueId)) {
            player.sendText {
                appendErrorPrefix()
                error("Du darfst dich nicht vom Sattel schmeißen lassen!")
            }
        }
        event.cancel()
    }

    @EventHandler
    fun onVehicleChange(event: VehicleEnterEvent) {
        val player = event.entered
        if (player !is Player) return
        if (event.vehicle.type != EntityType.NAUTILUS) return
        if (!RaceService.isInRace(player)) return
        if (RaceService.getRaceState() == RaceState.WAITING) return

        if (canSendMessage(player.uniqueId)) {
            player.sendText {
                appendErrorPrefix()
                error("Du sollst andere nicht Kapern!")
            }
        }
        event.cancel()
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!RaceService.isInRace(player)) return

        event.cancel()
    }

    @EventHandler
    fun onDropEvent(event: PlayerDropItemEvent) {
        val player = event.player
        if (!RaceService.isInRace(player)) return
        event.cancel()
    }


    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!RaceService.isInRace(player)) return

        val to = event.to
        val from = event.from

        if (from.x.toInt() == to.x.toInt() &&
            from.y.toInt() == to.y.toInt() &&
            from.z.toInt() == to.z.toInt()
        ) return

        val checkpoint = RegionService.getCheckpoint(player.location)
        val lap = RegionService.getStart(player.location)

        if (checkpoint != null) {
            if (checkpoint.id < ProgressService.getCheckpoint(player.uniqueId)){
                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast den Checkpoint ${checkpoint.id} erreicht!")
                }
            }
            if (canSendMessage(player.uniqueId)) {
                player.sendText {
                    appendErrorPrefix()
                    error("Du warst hier bereits.")
                }
            }
        }

        if (lap != null) {

        }
    }
}