package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.command.util.stringLocation
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Location

fun setLocationConfigCommand() = subcommand("set") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("config", "lobby", "start")
    locationArgument("location")
    rotationArgument("rotation")
    playerExecutor { player, args ->
        val config: String by args
        val location: Location by args
        val rotation: Rotation by args


        when (config) {
            "lobby" -> {
                SurfRaceConfig.edit {
                    lobbyWorld = location.world.name
                    lobbyX = location.x
                    lobbyY = location.y
                    lobbyZ = location.z
                    lobbyYaw = rotation.yaw
                    lobbyPitch = rotation.pitch
                }

                player.sendText {
                    appendSuccessPrefix()
                    success("Die Lobby Location wurde erfolgreich gesetzt!")
                    variableValue(stringLocation(location))
                }
            }

            "start" -> {
                SurfRaceConfig.edit {
                    startWorld = location.world.name
                    startX = location.x
                    startY = location.y
                    startZ = location.z
                    startYaw = rotation.yaw
                    startPitch = rotation.pitch
                }

                player.sendText {
                    appendSuccessPrefix()
                    success("Die Start Location wurde erfolgreich gesetzt!")
                    variableValue(stringLocation(location))
                }
            }

            else -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Ungültige Konfiguration! Bitte wähle entweder 'lobby' oder 'start'.")
                }
            }
        }
        SurfRaceConfig.save()
    }
}