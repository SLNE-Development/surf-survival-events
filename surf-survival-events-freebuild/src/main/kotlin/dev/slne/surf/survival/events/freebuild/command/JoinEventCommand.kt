package dev.slne.surf.survival.events.freebuild.command

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.paper.util.surfPlayer
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import dev.slne.surf.survival.events.freebuild.joinCooldown
import dev.slne.surf.survival.events.freebuild.permission.PermissionList
import dev.slne.surf.survival.events.freebuild.plugin

fun joinEventCommand() = commandTree("survivalevents") {
    withPermission(PermissionList.COMMAND_JOIN)
    playerExecutorSuspend { player, _ ->
        if (!FreebuildPartConfig.getConfig().enableSurvivalEventsNpc) {
            player.sendText {
                appendErrorPrefix()
                error("Derzeit findet kein Survival Event statt. Bitte versuche es später erneut.")
            }
            return@playerExecutorSuspend
        }

        val surfServer =
            SurfCoreApi.getServerByName(FreebuildPartConfig.getConfig().eventServerName)

        if (surfServer == null) {
            player.sendText {
                appendErrorPrefix()
                error("Der Survival Events Server ist derzeit nicht erreichbar. Bitte versuche es später erneut.")
            }
            return@playerExecutorSuspend
        }

        if (joinCooldown.getIfPresent(player.uniqueId) != null) {
            player.sendText {
                appendErrorPrefix()
                error("Bitte warte einen Moment, bevor du dies erneut versuchst.")
            }
            return@playerExecutorSuspend
        }

        joinCooldown.put(player.uniqueId, Unit)

        plugin.launch {
            val surfPlayer = player.surfPlayer
            val result = SurfCoreApi.sendPlayerAwaiting(surfPlayer, surfServer)

            if (!result.isSuccessful()) {
                player.sendText {
                    appendErrorPrefix()
                    error("Beim Verbinden zum Survival Event Server ist ein Fehler aufgetreten. Bitte versuche es später erneut. (${result.status})")
                }
            }
        }
    }
}