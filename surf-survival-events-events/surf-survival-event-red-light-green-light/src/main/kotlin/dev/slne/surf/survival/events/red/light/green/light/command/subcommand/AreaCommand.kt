package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaSelection
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaSelectionActionBar
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaSelectionSounds
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaTarget
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.entity.Player

fun CommandAPICommand.areaCommand() = subcommand("area") {
    subcommand("start") {
        withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ -> toggleAreaSelection(player, AreaTarget.START) }
    }

    subcommand("finish") {
        withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ -> toggleAreaSelection(player, AreaTarget.FINISH) }
    }
}

private fun toggleAreaSelection(player: Player, target: AreaTarget) {
    val activeTarget = AreaSelection.toggle(player.uniqueId, target)

    if (activeTarget != null) {
        AreaSelectionActionBar.start(player, activeTarget)
        AreaSelectionSounds.playActivated(player)

        player.sendText {
            appendSuccessPrefix()
            success("${activeTarget.displayName}-Area-Set-Modus aktiviert. Haue einen Block an für Pos1, den nächsten für Pos2.")
        }
    } else {
        AreaSelectionActionBar.stop(player.uniqueId)
        AreaSelectionSounds.playDeactivated(player)

        player.sendText {
            appendSuccessPrefix()
            success("Area-Set-Modus deaktiviert.")
        }
    }
}