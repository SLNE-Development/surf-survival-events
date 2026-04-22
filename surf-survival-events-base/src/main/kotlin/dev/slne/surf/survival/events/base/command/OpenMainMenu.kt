package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.inventory.framework.viewFrame
import dev.slne.surf.survival.events.base.menu.view.OverviewView
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry


fun openMainMenu() = commandAPICommand("events") {
    withPermission(PermissionRegistry.COMMAND_OPEN_MAIN_MENU)

    playerExecutor { player, _ ->
        viewFrame.open(OverviewView::class.java, player)
    }
}
