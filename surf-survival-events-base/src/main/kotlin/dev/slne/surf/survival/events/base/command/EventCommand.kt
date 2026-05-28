package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.survival.events.base.command.subcommand.changePlayerLimitCommand
import dev.slne.surf.survival.events.base.command.subcommand.eventMenuCommand
import dev.slne.surf.survival.events.base.command.subcommand.joinAsSpectatorCommand
import dev.slne.surf.survival.events.base.command.subcommand.kickPlayerCommand
import dev.slne.surf.survival.events.base.command.subcommand.leaveQueueCommand
import dev.slne.surf.survival.events.base.command.subcommand.showQueueCommand
import dev.slne.surf.survival.events.base.command.subcommand.removeNpc
import dev.slne.surf.survival.events.base.command.subcommand.spawnNpc
import dev.slne.surf.survival.events.base.command.subcommand.startEventCommand
import dev.slne.surf.survival.events.base.command.subcommand.stopEventCommand

fun openMainMenu() = commandAPICommand("event") {
    withSubcommands(
        startEventCommand(),
        stopEventCommand(),
        joinAsSpectatorCommand(),
        leaveQueueCommand(),
        showQueueCommand(),
        eventMenuCommand(),
        kickPlayerCommand(),
        changePlayerLimitCommand(),
        spawnNpc(),
        removeNpc(),
    )
    
    playerExecutor { _, _ -> }
}
