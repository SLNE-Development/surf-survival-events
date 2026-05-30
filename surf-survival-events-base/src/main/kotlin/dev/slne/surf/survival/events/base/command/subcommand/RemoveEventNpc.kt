package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.NpcService
import dev.slne.surf.survival.events.base.util.PermissionRegistry

fun CommandTree.removeNpc() = literalArgument("remove-npc") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->
        NpcService.hideNpc()

        sender.sendText {
            appendSuccessPrefix()
            success("Der NPC für das Event wurde entfernt.")
        }
    }
}