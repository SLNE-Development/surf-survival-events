package dev.slne.surf.survival.events.race.command.util

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.race.utils.PermissionList
import org.bukkit.Location
import java.util.*

data class RegionSelection(
    var pos1: Location? = null,
    var pos2: Location? = null
)

private val selections = mutableMapOf<UUID, RegionSelection>()

fun CommandAPICommand.createRegionCommand(
    name: String,
    requiresRotation: Boolean = false,
    onCreate: (
        pos1: Location,
        pos2: Location,
        rotation: Rotation?
    ) -> Unit
) = subcommand(name) {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("argument", "pos1", "pos2", "create")
    locationArgument("location", LocationType.BLOCK_POSITION)

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

                player.sendText {
                    appendSuccessPrefix()
                    success("Pos1:")
                    appendSpace()
                    variableValue(location.readableString(true))
                }
            }

            "pos2" -> {
                selection.pos2 = location

                player.sendText {
                    appendSuccessPrefix()
                    success("Pos2:")
                    appendSpace()
                    variableValue(location.readableString(true))
                }
            }

            "create" -> {
                val pos1 = selection.pos1
                val pos2 = selection.pos2

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

                onCreate(pos1, pos2, rotation)

                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast eine Region zwischen")
                    appendSpace()
                    variableValue(pos1.readableString(true))
                    appendSpace()
                    success("und")
                    appendSpace()
                    variableValue(pos2.readableString(true))
                    appendSpace()
                    success("erstellt.")
                }

                selections.remove(player.uniqueId)
            }
        }
    }
}