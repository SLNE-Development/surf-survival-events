package dev.slne.surf.survival.events.race.listener

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import io.papermc.paper.event.entity.EntityMoveEvent
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
    fun onEntityMove(event: EntityMoveEvent) {
        val player = event.entity.passengers.firstOrNull() as? Player ?: return
        println(player.name)
        if (!RaceService.isInRace(player)) return
        if (RaceService.getRaceState() != RaceState.RUNNING) {

            event.cancel()
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!RaceService.isInRace(player)) return
        if (RaceService.getRaceState() == RaceState.COUNTDOWN || RaceService.getRaceState() == RaceState.WAITING) {
            event.cancel()
        }
    }


    @EventHandler
    fun onVehicleExit(event: VehicleExitEvent) {
        val player = event.exited
        if (player !is Player) return

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

        if (!RaceService.isInRace(player)) return
        if (RaceService.getRaceState() != RaceState.RUNNING) return

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
        if (RaceService.getRaceState() != RaceState.RUNNING) return

        event.cancel()
    }

    @EventHandler
    fun onDropEvent(event: PlayerDropItemEvent){
        val player = event.player
        if (!RaceService.isInRace(player)) return
        event.cancel()
    }
}