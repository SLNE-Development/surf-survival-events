package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.base.config.SurfRaceConfig
import dev.slne.surf.survival.events.base.service.NpcService
import org.bukkit.Location

fun CommandTree.setEventManagerCommand() = literalArgument("set-event-manager") {
    locationArgument("location", LocationType.BLOCK_POSITION) {
        playerExecutor { player, args ->
            val location: Location by args

            SurfRaceConfig.edit {
                eventManager.clear()
                eventManager.add(
                    SurfRaceConfig.EventManagerConfig(
                        eventManagerWorld = location.world.name,

                        eventManagerX = location.x,
                        eventManagerY = location.y,
                        eventManagerZ = location.z,
                    )
                )
            }

            player.sendText {
                appendSuccessPrefix()
                success("Die Position des Event Managers wurde erfolgreich gesetzt!")
                appendSpace()
                variableValue(location.readableString(true))
            }

            SurfRaceConfig.save()

            NpcService.newPositionNpc(location)
        }
    }
}