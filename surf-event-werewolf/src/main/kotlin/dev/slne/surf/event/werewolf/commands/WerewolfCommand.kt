package dev.slne.surf.event.werewolf.commands

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.event.werewolf.commands.subcommands.startWerewolfCommand
import dev.slne.surf.event.werewolf.commands.subcommands.openGameWerewolfCommand
import dev.slne.surf.event.werewolf.commands.subcommands.stopWerewolfCommand
import dev.slne.surf.event.werewolf.commands.subcommands.joinWerewolfCommand

fun werewolfCommand() = commandAPICommand("werewolf"){

    openGameWerewolfCommand()
    startWerewolfCommand()
    stopWerewolfCommand()
    joinWerewolfCommand()

    playerExecutor { player, arguments ->

    }
}