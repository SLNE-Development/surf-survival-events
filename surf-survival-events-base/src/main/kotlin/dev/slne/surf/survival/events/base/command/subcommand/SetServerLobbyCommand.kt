package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.base.config.SurvivalEventsConfig
import dev.slne.surf.survival.events.base.util.PermissionRegistry
import org.bukkit.Location

internal fun CommandTree.setServerLobbyCommand() = literalArgument("set-server-lobby") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    locationArgument("location", LocationType.BLOCK_POSITION) {
        rotationArgument("rotation") {
            playerExecutor { player, args ->
                val location: Location by args
                val rotation: Rotation by args
                val lobbyLocation = location.setRotation(rotation.yaw, rotation.pitch)

                SurvivalEventsConfig.edit {
                    serverLobby = lobbyLocation
                }

                player.sendText {
                    appendSuccessPrefix()
                    success("Die Server-Lobby wurde erfolgreich gesetzt!")
                    appendSpace()
                    variableValue(lobbyLocation.readableString(showRotation = true))
                }
            }
        }
    }
}
