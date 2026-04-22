package dev.slne.surf.survival.events.paper.util.commands

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionRegistry : PermissionRegistry() {

    private const val PREFIX = "surf.survival.events"
    private const val COMMAND_PREFIX = "$PREFIX.command"


    val COMMAND_OPEN_MAIN_MENU = create("$COMMAND_PREFIX.open.main.menu")
    val COMMAND_GAME_SPECTATOR = create("$COMMAND_PREFIX.game.spectator")
    val COMMAND_GAME_ADMIN = create("$COMMAND_PREFIX.game.admin")
    val COMMAND_GAME_PLAYER = create("$COMMAND_PREFIX.game.player")
}