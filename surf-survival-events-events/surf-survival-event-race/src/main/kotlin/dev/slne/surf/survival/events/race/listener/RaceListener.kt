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
import java.util.*
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
        val player = event.exited as? Player ?: return

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
    fun onVehicleEnter(event: VehicleEnterEvent) {
        val player = event.entered as? Player ?: return

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
        val raceState = RaceService.getRaceState()

        if (!RaceService.isInRace(player)) return
        if (raceState != RaceState.RUNNING) return
        if (ProgressService.isFinished(uuid)) return

        val from = event.from
        val to = event.to

        if (
            from.blockX == to.blockX &&
            from.blockY == to.blockY &&
            from.blockZ == to.blockZ
        ) {
            return
        }

        val checkpointFrom = RegionService.getCheckpoint(from)
        val checkpointTo = RegionService.getCheckpoint(to)

        val startFrom = RegionService.getStart(from)
        val startTo = RegionService.getStart(to)

        val currentCheckpoint = ProgressService.getCheckpoint(uuid)

        val enteredNewCheckpoint = checkpointTo != null &&
                (checkpointFrom == null || checkpointFrom.id != checkpointTo.id)

        if (enteredNewCheckpoint && checkpointTo != null) {

            val expectedCheckpoint = RegionService.getNextExpectedCheckpointId(currentCheckpoint)

            if (expectedCheckpoint != null && checkpointTo.id == expectedCheckpoint) {

                ProgressService.checkpointUp(uuid, checkpointTo.id)

                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast den Checkpoint ${checkpointTo.id} erreicht!")
                }

            } else if (canSendMessage(uuid)) {

                player.sendText {
                    appendErrorPrefix()
                    error("Falscher Checkpoint!")
                }
            }
        }

        val enteredStart = startTo != null &&
                (startFrom == null || startFrom != startTo)

        if (enteredStart) { // TODO: Fix laps

            val highestCheckpoint = RegionService.getHighestCheckpointId() ?: return

            println("Highest Checkpoint: $highestCheckpoint")

            if (ProgressService.getCheckpoint(uuid) != highestCheckpoint) {
                println("Player checkpoint: ${ProgressService.getCheckpoint(uuid)}")
                return
            }

            println("Player entered start region, valid lap up.")

            ProgressService.lapUp(uuid)

            val config = SurfRaceConfig.getConfig()
            val lap = ProgressService.getLap(uuid)

            if (lap >= config.laps) {

                ProgressService.setFinished(uuid, true)
                ProgressService.addPlace(uuid)

                val place = ProgressService.getPlace(uuid)

                RaceService.getSpectatorPlayers().forEach { spectatorUuid ->
                    val spectator = Bukkit.getPlayer(spectatorUuid) ?: return@forEach

                    spectator.sendText {
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
                success("Du bist jetzt in Runde")
                appendSpace()
                variableValue(lap + 1)
                appendSpace()
                success(".")
            }
        }
    }
}