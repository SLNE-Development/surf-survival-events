package dev.slne.surf.survival.events.paper.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.api.paper.inventory.framework.viewFrame
import dev.slne.surf.survival.events.paper.menu.view.OverviewView
import dev.slne.surf.survival.events.paper.util.commands.PermissionRegistry


fun openMainMenu() = commandAPICommand("events") {
    withPermission(PermissionRegistry.COMMAND_OPEN_MAIN_MENU)

    playerExecutorSuspend { player, _ ->
        viewFrame.open(OverviewView::class.java, player)
    }
}
