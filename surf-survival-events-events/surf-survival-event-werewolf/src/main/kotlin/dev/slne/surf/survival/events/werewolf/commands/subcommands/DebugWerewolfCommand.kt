package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements

fun debugWerewolfCommand() = subcommand("debug") {
    withRequirement { sender -> WerewolfCommandRequirements.canDebugSkipNightStep(sender) }
    subcommand(skipNightStepWerewolfCommand())
}
