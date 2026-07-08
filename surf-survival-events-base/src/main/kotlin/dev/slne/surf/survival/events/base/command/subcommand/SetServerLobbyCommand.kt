package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.base.config.SurvivalEventsConfig
import dev.slne.surf.survival.events.base.util.PermissionRegistry

internal fun CommandTree.setServerLobbyCommand() = literalArgument("set-server-lobby") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    playerExecutor { player, _ ->
        val lobbyLocation = player.location

        SurvivalEventsConfig.edit {
            serverLobby = lobbyLocation
        }
        SurvivalEventsConfig.save()

        player.sendText {
            appendSuccessPrefix()
            success("Die Server-Lobby wurde erfolgreich gesetzt!")
            appendSpace()
            variableValue(lobbyLocation.readableString(showRotation = true))
        }
    }
}
