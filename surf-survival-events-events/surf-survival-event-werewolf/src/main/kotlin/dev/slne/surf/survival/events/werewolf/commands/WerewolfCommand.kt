package dev.slne.surf.survival.events.werewolf.commands

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.amorWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.debugWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.doctorWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.girlWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.inspectWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.joinWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.killWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.openGameWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.priestWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.serialKillerWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.startWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.stopWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.voteWerewolfCommand
import dev.slne.surf.survival.events.werewolf.commands.subcommands.witchWerewolfCommand

fun werewolfCommand() = commandAPICommand("werewolf"){
    subcommand(voteWerewolfCommand())
    subcommand(killWerewolfCommand())
    subcommand(amorWerewolfCommand())
    subcommand(girlWerewolfCommand())
    subcommand(inspectWerewolfCommand())
    subcommand(doctorWerewolfCommand())
    subcommand(priestWerewolfCommand())
    subcommand(serialKillerWerewolfCommand())
    subcommand(witchWerewolfCommand())
    subcommand(debugWerewolfCommand())

    subcommand(startWerewolfCommand())
    subcommand(stopWerewolfCommand())
    subcommand(openGameWerewolfCommand())
    subcommand(joinWerewolfCommand())
}
