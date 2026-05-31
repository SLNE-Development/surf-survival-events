package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.NpcService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun CommandTree.spawnNpc() = literalArgument("spawn-npc") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->
        NpcService.hideNpc()
        NpcService.showNpc()

        sender.sendText {
            appendSuccessPrefix()
            success("Event-NPC wurde gespawnt.")
        }
    }
}