package dev.slne.surf.event.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.subcommand

fun debugWerewolfCommand() = subcommand("debug") {
    subcommand(skipNightStepWerewolfCommand())
}
