package dev.slne.surf.survival.events.race.command.subcommand


import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import kotlin.system.measureTimeMillis

fun CommandTree.surfModToolsReloadCommand() {
    literalArgument("reload") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        anyExecutor { sender, _ ->
            val ms = measureTimeMillis {
                SurfRaceConfig.reloadFromFile()
            }

            sender.sendText {
                appendSuccessPrefix()
                success("Das Plugin wurde erfolgreich neu geladen ")
                spacer("({$ms}ms)")
            }
        }
    }
}