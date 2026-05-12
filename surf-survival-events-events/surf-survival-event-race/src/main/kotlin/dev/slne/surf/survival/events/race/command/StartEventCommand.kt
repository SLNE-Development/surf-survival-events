package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Bukkit
import org.bukkit.Location

fun startEventCommand() = subcommand("event") {
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

            RaceService.getRacePlayers().forEach { uuid ->

                val target = Bukkit.getPlayer(uuid)
                val config = SurfRaceConfig.getConfig()
                val world = Bukkit.getWorld(config.startWorld)
                val location =
                    Location(world, config.startX, config.startY, config.startZ, config.startYaw, config.startPitch)

                if (target != null) {
                    target.teleportAsync(location)
                    target.sendText {
                        appendSuccessPrefix()
                        success("Du erhältst gleich deine Nautilus!")
                    }
                }
            }
            RaceService.setRaceState(RaceState.WAITING)

            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            error("Du kannst den Command nicht mehr ausführen.")
            appendNewline()
            error("Aktuelle State:")
            appendSpace()
            variableValue(RaceService.getRaceState().name)
        }
    }
}

