package dev.slne.surf.survival.events.paper.util.commands

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionRegistry : PermissionRegistry() {

    private const val PREFIX = "surf.survival.events"
    private const val COMMAND_PREFIX = "$PREFIX.command"


    val COMMAND_OPEN_MAIN_MENUE = create("$COMMAND_PREFIX.open.main.menue")
    val COMMAND_GAME_MANAGAER = create("$COMMAND_PREFIX.game.manager")
    val COMMAND_GAME_ADMIN = create("$COMMAND_PREFIX.game.admin")
    val COMMAND_GAME_PLAYER = create("$COMMAND_PREFIX.game.player")
}