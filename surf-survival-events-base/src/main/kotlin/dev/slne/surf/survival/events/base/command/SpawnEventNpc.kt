package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.NpcService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun spawnEventNpc() = commandAPICommand("spawn-event-npc") {
    withPermission(PermissionRegistry.COMMAND_ADMIN)
    anyExecutor { sender, _ ->
        NpcService.hideNpc()
        NpcService.showNpc()

        sender.sendText {
            appendSuccessPrefix()
            success("Der Event-NPC wurde gespawnt.")
        }
    }
}