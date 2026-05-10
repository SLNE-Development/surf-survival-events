package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun startEventCommand() = subcommand("event") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        player.sendText {
            appendSuccessPrefix()
            success("Du hast dir die Spieler geholt")
        }
    }
}