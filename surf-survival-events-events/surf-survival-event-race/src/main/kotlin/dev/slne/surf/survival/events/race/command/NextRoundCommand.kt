package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun nextRoundCommand() = subcommand("next-round") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    integerArgument("rawInt", 1)

    playerExecutor { player, args ->
        val rawInt: Int by args
        RaceService.nextRound(rawInt)
        player.sendText {
            appendSuccessPrefix()
            success("Die Nächste Runde startet...")
        }
    }
}