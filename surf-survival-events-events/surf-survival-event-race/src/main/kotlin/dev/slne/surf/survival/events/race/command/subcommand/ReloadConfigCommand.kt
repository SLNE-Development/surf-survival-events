package dev.slne.surf.survival.events.race.command.subcommand


import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionList
import kotlin.system.measureTimeMillis

fun CommandAPICommand.surfModToolsReloadCommand() = subcommand("reload") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->
        val ms = measureTimeMillis {
            RaceConfig.reloadFromFile()
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Das Plugin wurde erfolgreich neu geladen ")
            spacer("(${ms}ms)")
        }
    }
}