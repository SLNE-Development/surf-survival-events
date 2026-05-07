package dev.slne.surf.event.werewolf.commands

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.event.werewolf.commands.subcommands.*

fun werewolfCommand() = commandAPICommand("werewolf"){

    subcommand(openGameWerewolfCommand())
    subcommand(startWerewolfCommand())
    subcommand(stopWerewolfCommand())
    subcommand(joinWerewolfCommand())
    subcommand(voteWerewolfCommand())
    subcommand(killWerewolfCommand())
    subcommand(amorWerewolfCommand())
    subcommand(girlWerewolfCommand())
    subcommand(inspectWerewolfCommand())
    subcommand(doctorWerewolfCommand())
    subcommand(priestWerewolfCommand())
    subcommand(serialKillerWerewolfCommand())
    subcommand(witchWerewolfCommand())

    playerExecutor { player, arguments ->

    }
}
