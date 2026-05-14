package dev.slne.surf.survival.events.race.command


import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.command.util.stringLocation
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Location
import java.util.UUID

private val barrierPos1 = mutableMapOf<UUID, Location>()
private val barrierPos2 = mutableMapOf<UUID, Location>()

fun setBarrierCommand() = subcommand("set-barrier") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("argument", "pos1", "pos2", "create")
    locationArgument("location", LocationType.BLOCK_POSITION)

    playerExecutor { player, args ->
        val argument: String by args
        val location: Location by args


        when (argument) {
            "pos1" -> {

                barrierPos1[player.uniqueId] = location

                player.sendText {
                    appendSuccessPrefix()
                    success("Pos1:")
                    appendSpace()
                    variableValue(stringLocation(location))
                }
            }

            "pos2" -> {

                barrierPos2[player.uniqueId] = location

                player.sendText {
                    appendSuccessPrefix()
                    success("Pos2:")
                    appendSpace()
                    variableValue(stringLocation(location))
                }
            }

            "create" -> {
                val pos1 = barrierPos1[player.uniqueId]
                val pos2 = barrierPos2[player.uniqueId]

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
                    barrier.clear()
                    barrier.add(
                        SurfRaceConfig.Barrier(

                            world = pos1.world.name,
                            x1 = pos1.x,
                            y1 = pos1.y,
                            z1 = pos1.z,

                            x2 = pos2.x,
                            y2 = pos2.y,
                            z2 = pos2.z,
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

                barrierPos1.remove(player.uniqueId)
                barrierPos2.remove(player.uniqueId)

                SurfRaceConfig.save()
            }
        }
    }
}
