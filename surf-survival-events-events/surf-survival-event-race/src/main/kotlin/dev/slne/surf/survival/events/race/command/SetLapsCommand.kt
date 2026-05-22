package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun setLapsCommand() = subcommand("setlaps") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
}