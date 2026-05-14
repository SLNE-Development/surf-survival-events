package dev.slne.surf.survival.events.race.command


import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material

fun startRaceCommand() = subcommand("start") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        if (RaceService.getRaceState() == RaceState.DEACTIVATED) {
            player.sendText {
                appendErrorPrefix()
                error("Race ist nicht Aktiv.")
            }
            return@playerExecutor
        }

        if (RaceService.getRaceState() == RaceState.LOBBY) {

            player.sendText {
                appendSuccessPrefix()
                success("Du hast dir die Spieler geholt")
            }

            RegionService.fillBlocks(Material.BARRIER)

            RaceService.getRacePlayers().forEach { uuid ->
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                RaceService.setPlayerOnNautilus(player)
            }

            SurfRaceConfig.getConfig().start.forEach { start ->
                val location = midLocation(start)

                RaceService.getRacePlayers().forEach { uuid ->

                    Bukkit.getPlayer(uuid)?.let { target ->
                        target.teleportAsync(location)

                        target.sendText {
                            appendSuccessPrefix()
                            success("Du erhältst gleich deine Nautilus.")
                        }
                    }
                }
            }
            RaceService.setRaceState(RaceState.WAITING)

            return@playerExecutor
        }

        if (RaceService.getRaceState() == RaceState.WAITING) {

            player.sendText {
                appendSuccessPrefix()
                success("Das Spiel startet...")
            }
            RaceService.setRaceState(RaceState.COUNTDOWN)
            RaceService.startCountdown()
            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            error("Du kannst den Command nicht ausführen.")
            appendNewline()
            error("Aktuelle State:")
            appendSpace()
            variableValue(RaceService.getRaceState().name)
        }
    }
}

private fun midLocation(start: SurfRaceConfig.Start): Location {
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
