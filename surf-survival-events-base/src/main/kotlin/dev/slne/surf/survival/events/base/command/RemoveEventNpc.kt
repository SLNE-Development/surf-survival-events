package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.npc.service.NpcService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun removeEventNpc() = commandAPICommand("remove-event-npc") {
    withPermission(PermissionRegistry.COMMAND_ADMIN)
    anyExecutor { sender, _ ->
        NpcService.hideNpc()

        sender.sendText {
            appendSuccessPrefix()
            success("Der Event-NPC wurde entfernt.")
        }
    }
}