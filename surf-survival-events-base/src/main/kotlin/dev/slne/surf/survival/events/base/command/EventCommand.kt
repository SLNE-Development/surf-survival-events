package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.survival.events.base.command.subcommand.*

fun eventCommand() = commandTree("event") {
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
