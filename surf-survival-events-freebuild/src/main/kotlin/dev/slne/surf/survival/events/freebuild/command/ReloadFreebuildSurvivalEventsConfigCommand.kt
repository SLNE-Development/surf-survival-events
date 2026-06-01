package dev.slne.surf.survival.events.freebuild.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import dev.slne.surf.survival.events.freebuild.permission.PermissionList

fun reloadFreebuildSurvivalEventsConfigCommand() =
    commandTree("reloadfreebuildsurvivaleventsconfig") {
        withPermission(PermissionList.COMMAND_RELOAD)

        anyExecutor { sender, _ ->
            FreebuildPartConfig.manager.reloadFromFile()

            sender.sendText {
                appendSuccessPrefix()
                success("Die Freebuild Survival Events Konfiguration wurde neu geladen.")
            }
        }
    }