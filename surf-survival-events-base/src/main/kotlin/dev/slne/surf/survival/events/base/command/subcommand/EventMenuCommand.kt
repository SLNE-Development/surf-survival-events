package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.paper.inventory.framework.viewFrame
import dev.slne.surf.survival.events.base.menu.view.OverviewView
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun eventMenuCommand() = subcommand("menu") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        viewFrame.open(OverviewView::class.java, player)
    }
}