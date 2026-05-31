package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.locationArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.rotationArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.jorel.commandapi.wrappers.Rotation
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.race.config.RaceConfig
import org.bukkit.Location

fun CommandAPICommand.setSpectatorCommand() = subcommand("set-spectator") {
    locationArgument("location", LocationType.BLOCK_POSITION)
    rotationArgument("rotation")
    playerExecutor { player, args ->
        val location: Location by args
        val rotation: Rotation by args

        val spectatorLocation = location.setRotation(rotation.yaw, rotation.pitch)

        RaceConfig.edit {
            eventWorld = location.world.name
            this.spectatorLocation = spectatorLocation
        }
        RaceConfig.save()

        player.sendText {
            appendSuccessPrefix()
            success("Die Spectator-Location wurde erfolgreich gesetzt!")
            appendSpace()
            variableValue(spectatorLocation.readableString(true))
        }
    }
}
