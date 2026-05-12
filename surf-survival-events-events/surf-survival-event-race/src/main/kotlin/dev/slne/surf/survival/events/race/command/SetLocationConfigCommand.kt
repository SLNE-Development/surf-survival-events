package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.command.util.stringLocation
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Location

fun setLocationConfigCommand() = subcommand("set-location") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    literalArgument("config", "lobby")
    locationArgument("location")
    rotationArgument("rotation")
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