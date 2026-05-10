package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun startCountDownCommand() = subcommand("CountDown") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    anyExecutor { sender, _ ->  }
}