package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Location

fun setLocationConfigCommand() = subcommand("set") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("config", "lobby", "start", "checkpoint")
    locationArgument("location")
    anyExecutor { sender, args ->
        val config: String by args
        val location: Location by args

        when (config) {
            "lobby" -> {
                SurfRaceConfig.edit {
                    lobbyWorld = location.world.name
                    lobbyX = location.x
                    lobbyY = location.y
                    lobbyZ = location.z
                    lobbyYaw = location.yaw
                    lobbyPitch = location.pitch
                }
                SurfRaceConfig.save()
                sender.sendText {
                    appendSuccessPrefix()
                    success("Die Lobby Location wurde erfolgreich gesetzt!")
                    variableValue(location.toString())
                }
            }

            "start" -> {
                SurfRaceConfig.edit {
                    startWorld = location.world.name
                    startX = location.x
                    startY = location.y
                    startZ = location.z
                    startYaw = location.yaw
                    startPitch = location.pitch
                }
                SurfRaceConfig.save()
                sender.sendText {
                    appendSuccessPrefix()
                    success("Die Start Location wurde erfolgreich gesetzt!")
                    variableValue(location.toString())
                }
            }

            "checkpoint" -> {
                SurfRaceConfig.edit {
                    //checkPoints.add(location)
                    //TODO: Make it editable
                }
                SurfRaceConfig.save()
            }
        }
    }
}