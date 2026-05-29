package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import org.bukkit.Location

fun setLobbyCommand() = subcommand("set-lobby") {
    locationArgument("location", LocationType.BLOCK_POSITION)
    rotationArgument("rotation")
    playerExecutor { player, args ->

        val location: Location by args
        val rotation: Rotation by args

        SurfRaceConfig.edit {

            lobby.clear()

            lobby.add(
                SurfRaceConfig.LobbyConfig(
                    lobbyWorld = location.world.name,

                    lobbyX = location.x,
                    lobbyY = location.y,
                    lobbyZ = location.z,

                    lobbyYaw = rotation.yaw,
                    lobbyPitch = rotation.pitch
                )
            )
        }

        player.sendText {
            appendSuccessPrefix()
            success("Die Lobby Location wurde erfolgreich gesetzt!")
            variableValue(location.readableString(true))
        }

        SurfRaceConfig.save()
    }
}