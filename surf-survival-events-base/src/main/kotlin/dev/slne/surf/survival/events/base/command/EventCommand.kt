package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.survival.events.base.command.subcommand.*

internal fun eventCommand() = commandTree("survivalevents") {
    startEventCommand()
    stopEventCommand()
    joinAsSpectatorCommand()
    leaveEventCommand()
    participantsCommand()
    eventMenuCommand()
    kickPlayerCommand()
    setServerLobbyCommand()
    reloadCommand()
}
