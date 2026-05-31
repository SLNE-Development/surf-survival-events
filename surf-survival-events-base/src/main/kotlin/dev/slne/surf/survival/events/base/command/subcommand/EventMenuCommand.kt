package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.survival.events.base.menu.view.eventGamesOverviewView
import dev.slne.surf.survival.events.base.util.PermissionRegistry

internal fun CommandTree.eventMenuCommand() = literalArgument("menu") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        eventGamesOverviewView.open(player)
    }
}