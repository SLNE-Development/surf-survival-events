package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.base.util.toGamePosition
import dev.slne.surf.survival.events.race.config.RaceConfig
import org.bukkit.Location

fun CommandAPICommand.setLobbyCommand() = subcommand("set-lobby") {
    locationArgument("location", LocationType.BLOCK_POSITION)
    rotationArgument("rotation")
    playerExecutor { player, args ->
        val location: Location by args
        val rotation: Rotation by args

        val lobby = location.setRotation(rotation.yaw, rotation.pitch)

        RaceConfig.edit {
            roundLobbyLocation = lobby.toGamePosition()
        }
        RaceConfig.save()

        player.sendText {
            appendSuccessPrefix()
            success("Die Race-Round-Lobby wurde erfolgreich gesetzt!")
            appendSpace()
            variableValue(lobby.readableString(true))
        }
    }
}
