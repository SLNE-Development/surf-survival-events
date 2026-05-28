package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.command.util.createRegionCommand
import dev.slne.surf.survival.events.race.command.util.stringLocation
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Location

fun CommandTree.setCommands() {

    literalArgument("set") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        literalArgument("laps") {
            integerArgument("value", 1) {
                anyExecutor { sender, args ->
                    val value: Int by args

                    SurfRaceConfig.edit {
                        laps = value
                    }
                    SurfRaceConfig.save()

                    sender.sendText {
                        appendSuccessPrefix()
                        success("Die Anzahl der Runden wurde auf")
                        appendSpace()
                        variableValue(value.toString())
                        appendSpace()
                        success("gesetzt.")
                    }
                }
            }
        }
        createRegionCommand(
            name = "start",
            requiresRotation = true
        ) { pos1,
            pos2,
            rotation ->

            rotation ?: return@createRegionCommand

            SurfRaceConfig.edit {

                start.clear()

                start.add(
                    SurfRaceConfig.Start(

                        world = pos1.world.name,

                        x1 = pos1.x,
                        y1 = pos1.y,
                        z1 = pos1.z,

                        x2 = pos2.x,
                        y2 = pos2.y,
                        z2 = pos2.z,

                        yaw = rotation.yaw,
                        pitch = rotation.pitch
                    )
                )
            }

            SurfRaceConfig.save()
        }
        createRegionCommand("barrier") { pos1, pos2, _ ->
            SurfRaceConfig.edit {

                barrier.clear()

                barrier.add(
                    SurfRaceConfig.Barrier(
                        world = pos1.world.name,

                        x1 = pos1.x,
                        y1 = pos1.y,
                        z1 = pos1.z,

                        x2 = pos2.x,
                        y2 = pos2.y,
                        z2 = pos2.z
                    )
                )
            }

            SurfRaceConfig.save()
        }
        createRegionCommand("checkpoint") { pos1, pos2, _ ->
            val nextNumber =
                (SurfRaceConfig.getConfig().checkPoints.maxOfOrNull { it.id } ?: 0) + 1

            SurfRaceConfig.edit {

                checkPoints.add(
                    SurfRaceConfig.Checkpoint(

                        id = nextNumber,

                        world = pos1.world.name,

                        x1 = pos1.x,
                        y1 = pos1.y,
                        z1 = pos1.z,

                        x2 = pos2.x,
                        y2 = pos2.y,
                        z2 = pos2.z
                    )
                )
            }

            SurfRaceConfig.save()
        }
        literalArgument("lobby") {
            locationArgument("location", LocationType.BLOCK_POSITION) {
                rotationArgument("rotation") {
                    playerExecutor { player, args ->
                        val location: Location by args
                        val rotation: Rotation by args

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
                        SurfRaceConfig.save()
                    }
                }
            }
        }
    }
}
