package dev.slne.surf.survival.events.race.command.util

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.wrappers.Rotation
import org.bukkit.Location
import java.util.UUID

data class RegionSelection(
    var pos1: Location? = null,
    var pos2: Location? = null
)

private val selections = mutableMapOf<UUID, RegionSelection>()

fun CommandTree.createRegionCommand(
    name: String,
    requiresRotation: Boolean = false,
    onCreate: (Location, Location, Rotation?) -> Unit
) {
    literalArgument(name) {
        literalArgument("argument") {
            multiLiteralArgument("type", "pos1", "pos2", "create") {
                locationArgument("location", LocationType.BLOCK_POSITION) {

                    if (requiresRotation) {
                        rotationArgument("rotation")
                    }

                    playerExecutor { player, args ->

                        val argument: String by args
                        val location: Location by args

                        val rotation =
                            if (requiresRotation) args["rotation"] as Rotation
                            else null

                        val selection = selections.getOrPut(player.uniqueId) {
                            RegionSelection()
                        }

                        when (argument) {

                            "pos1" -> {
                                selection.pos1 = location
                                player.sendMessage("Pos1 gesetzt")
                            }

                            "pos2" -> {
                                selection.pos2 = location
                                player.sendMessage("Pos2 gesetzt")
                            }

                            "create" -> {

                                val pos1 = selection.pos1
                                val pos2 = selection.pos2

                                if (pos1 == null || pos2 == null) return@playerExecutor

                                onCreate(pos1, pos2, rotation)
                                selections.remove(player.uniqueId)
                            }
                        }
                    }
                }
            }
        }
    }
}