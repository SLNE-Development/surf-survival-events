package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.command.util.stringLocation
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Location
import dev.jorel.commandapi.wrappers.Rotation
import java.util.UUID

private val startPos1 = mutableMapOf<UUID, Location>()
private val startPos2 = mutableMapOf<UUID, Location>()

fun setStartCommand() = subcommand("set-start") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("argument", "pos1", "pos2", "create")
    locationArgument("location", LocationType.BLOCK_POSITION, optional = true)
    rotationArgument("rotation")

    playerExecutor { player, args ->
        val argument: String by args
        val location: Location by args
        val rotation: Rotation by args


        when (argument) {
            "pos1" -> {

                if (location == null) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du musst eine Location angeben!")
                    }
                    return@playerExecutor
                }

                startPos1[player.uniqueId] = location

                player.sendText {
                    appendSuccessPrefix()
                    success("Pos1:")
                    appendSpace()
                    variableValue(stringLocation(location))
                }
            }

            "pos2" -> {

                if (location == null) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du musst eine Location angeben!")
                    }
                    return@playerExecutor
                }

                startPos2[player.uniqueId] = location

                player.sendText {
                    appendSuccessPrefix()
                    success("Pos2:")
                    appendSpace()
                    variableValue(stringLocation(location))
                }
            }

            "create" -> {

                if (rotation == null) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du musst eine Rotation angeben!")
                    }
                    return@playerExecutor
                }

                val pos1 = startPos1[player.uniqueId]
                val pos2 = startPos2[player.uniqueId]

                if (pos1 == null || pos2 == null) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du musst erst pos1 und pos2 setzen!")
                    }
                    return@playerExecutor
                }

                if (pos1.world != pos2.world) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Beide Positionen müssen in derselben Welt sein!")
                    }
                    return@playerExecutor
                }

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
                            pitch = rotation.pitch,
                        )
                    )
                }

                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast eine Region zwischen")
                    appendSpace()
                    variableValue(stringLocation(pos1))
                    appendSpace()
                    success("und")
                    appendSpace()
                    variableValue(stringLocation(pos2))
                    appendSpace()
                    success("erstellt.")
                }

                startPos1.remove(player.uniqueId)
                startPos2.remove(player.uniqueId)

                SurfRaceConfig.save()
            }
        }
    }
}
