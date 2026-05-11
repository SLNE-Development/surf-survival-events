package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Bukkit
import org.bukkit.Location

fun startEventCommand() = subcommand("event") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        if (!RaceService.isGameActive()) {
            player.sendText {
                appendErrorPrefix()
                error("Race ist nicht Aktiv.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendSuccessPrefix()
            success("Du hast dir die Spieler geholt")
        }

        RaceService.getRacePlayers().forEach { uuid ->

            val target = Bukkit.getPlayer(uuid)
            val config = SurfRaceConfig.getConfig()
            val world = Bukkit.getWorld(config.startWorld)
            val location = Location(world, config.startX, config.startY, config.startZ, config.startYaw, config.startPitch)

            if (target != null) {
                target.teleportAsync(location)
                target.sendText {
                    appendSuccessPrefix()
                    success("Du hast deine Nautilus erhalten warte auf den Start!")
                }
            }
        }
    }
}

