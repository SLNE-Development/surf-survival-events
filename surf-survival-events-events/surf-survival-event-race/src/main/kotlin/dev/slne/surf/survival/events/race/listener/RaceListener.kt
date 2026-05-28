package dev.slne.surf.survival.events.race.listener

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.service.ProgressService
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import org.bukkit.Bukkit
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
        val uuid = player.uniqueId

        if (!RaceService.isInRace(player)) return
        if (RaceService.getRaceState() != RaceState.RUNNING) return
        if (ProgressService.isFinished(uuid)) return

        val from = event.from
        val to = event.to

        if (
            from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ
        ) return

        val checkpointFrom = RegionService.getCheckpoint(from)
        val checkpointTo = RegionService.getCheckpoint(to)

        val startFrom = RegionService.getStart(from)
        val startTo = RegionService.getStart(to)

        val currentCheckpoint = ProgressService.getCheckpoint(uuid)

        if (checkpointFrom == null && checkpointTo != null) {

            val expectedCheckpoint = currentCheckpoint + 1

            if (checkpointTo.id == expectedCheckpoint) {

                ProgressService.checkpointUp(uuid)

                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast den Checkpoint ${checkpointTo.id} erreicht!")
                }

            } else {

                if (canSendMessage(uuid)) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Falscher Checkpoint!")
                    }
                }
            }
        }

        if (startFrom == null && startTo != null) {

            val config = SurfRaceConfig.getConfig()
            val highestCheckpoint = config.checkPoints.maxOfOrNull { it.id } ?: return

            if (ProgressService.getCheckpoint(uuid) == highestCheckpoint) {

                ProgressService.lapUp(uuid)

                val lap = ProgressService.getLap(uuid)

                if (lap >= config.laps) {

                    ProgressService.setFinished(uuid, true)
                    ProgressService.addPlace(uuid)

                    val place = ProgressService.getPlace(uuid)

                    RaceService.getSpectatorPlayers().forEach { uuid ->
                        val playerSpectator = Bukkit.getPlayer(uuid) ?: return@forEach
                        playerSpectator.sendText {
                            appendInfoPrefix()
                            variableValue(player.name)
                            appendSpace()
                            info("hat das Rennen auf Platz")
                            appendSpace()
                            variableValue("$place")
                            appendSpace()
                            info("beendet!")
                        }
                    }

                    player.sendText {
                        appendSuccessPrefix()
                        success("Du hast das Rennen auf Platz")
                        appendSpace()
                        variableValue(place)
                        appendSpace()
                        success("beendet!")
                    }
                    return
                }

                ProgressService.checkpointReset(uuid)

                player.sendText {
                    appendSuccessPrefix()
                    success("Du bist jetzt in Runde ${lap + 1}!")
                }
            }
        }

    }
}