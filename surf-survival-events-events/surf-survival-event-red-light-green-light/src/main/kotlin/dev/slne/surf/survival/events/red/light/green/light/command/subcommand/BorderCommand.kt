package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import dev.slne.surf.survival.events.red.light.green.light.visual.RlglZoneBorder
import org.bukkit.command.CommandSender

fun CommandAPICommand.borderCommand() = subcommand("border") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)

    subcommand("start") {
        anyExecutor { sender, _ -> reportToggle(sender, "Start-Zone", RlglZoneBorder.toggleStartZone()) }
    }

    subcommand("finish") {
        anyExecutor { sender, _ -> reportToggle(sender, "Ziel-Zone", RlglZoneBorder.toggleFinishZone()) }
    }

    subcommand("playarea") {
        anyExecutor { sender, _ -> reportToggle(sender, "Play-Area", RlglZoneBorder.togglePlayArea()) }
    }
}

private fun reportToggle(sender: CommandSender, zoneName: String, enabled: Boolean) {
    sender.sendText {
        appendSuccessPrefix()
        variableValue(zoneName)
        appendSpace()
        success("Partikel-Grenze ist jetzt")
        appendSpace()
        variableValue(if (enabled) "AN" else "AUS")
        success(".")
    }
}