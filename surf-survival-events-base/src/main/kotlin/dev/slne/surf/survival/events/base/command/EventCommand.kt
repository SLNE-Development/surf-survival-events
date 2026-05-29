package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.survival.events.base.command.subcommand.changePlayerLimitCommand
import dev.slne.surf.survival.events.base.command.subcommand.eventMenuCommand
import dev.slne.surf.survival.events.base.command.subcommand.joinAsSpectatorCommand
import dev.slne.surf.survival.events.base.command.subcommand.kickPlayerCommand
import dev.slne.surf.survival.events.base.command.subcommand.leaveQueueCommand
import dev.slne.surf.survival.events.base.command.subcommand.showQueueCommand
import dev.slne.surf.survival.events.base.command.subcommand.removeNpc
import dev.slne.surf.survival.events.base.command.subcommand.setEventManagerCommand
import dev.slne.surf.survival.events.base.command.subcommand.spawnNpc
import dev.slne.surf.survival.events.base.command.subcommand.startEventCommand
import dev.slne.surf.survival.events.base.command.subcommand.stopEventCommand

fun openMainMenu() = commandTree("event") {
        startEventCommand()
        stopEventCommand()
        joinAsSpectatorCommand()
        leaveQueueCommand()
        showQueueCommand()
        eventMenuCommand()
        kickPlayerCommand()
        changePlayerLimitCommand()
        spawnNpc()
        removeNpc()
        setEventManagerCommand()
}
