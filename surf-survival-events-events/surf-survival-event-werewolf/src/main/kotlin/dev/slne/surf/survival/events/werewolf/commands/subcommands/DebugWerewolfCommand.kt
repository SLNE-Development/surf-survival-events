package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.subcommand

fun debugWerewolfCommand() = subcommand("debug") {
    subcommand(skipNightStepWerewolfCommand())
}
