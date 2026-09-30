package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.base.util.toGamePosition
import dev.slne.surf.survival.events.red.light.green.light.config.RlglConfig
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.Location

fun CommandAPICommand.setStartSpawnCommand() = subcommand("set-start-spawn") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    locationArgument("location", LocationType.BLOCK_POSITION)
    rotationArgument("rotation")

    playerExecutor { player, args ->
        val location: Location by args
        val rotation: Rotation by args

        val startSpawn = location.setRotation(rotation.yaw, rotation.pitch)

        RlglConfig.edit {
            this.startSpawn = startSpawn.toGamePosition()
        }

        player.sendText {
            appendSuccessPrefix()
            success("Der Start-Spawn wurde erfolgreich gesetzt!")
            appendSpace()
            variableValue(startSpawn.readableString(true))
        }
    }
}