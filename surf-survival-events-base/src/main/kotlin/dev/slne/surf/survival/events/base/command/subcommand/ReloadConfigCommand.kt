package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.config.SurvivalEventsConfig
import dev.slne.surf.survival.events.base.util.PermissionRegistry
import kotlin.time.measureTime

internal fun CommandTree.reloadCommand() = literalArgument("reload") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->
        val duration = measureTime {
            SurvivalEventsConfig.reloadFromFile()
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Die Konfiguration wurde erfolgreich neu geladen ")
            spacer("($duration)")
        }
    }
}