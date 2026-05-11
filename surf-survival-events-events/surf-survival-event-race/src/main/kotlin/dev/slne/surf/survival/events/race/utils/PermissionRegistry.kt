package dev.slne.surf.survival.events.race.utils

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionRegistry : PermissionRegistry() {
    private const val PREFIX = "surf.survival.events.race"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    val COMMAND_ADMIN = create("$COMMAND_PREFIX.admin")
    val COMMAND_COMMUNITY_MANAGER = create("$COMMAND_PREFIX.community_manager")
    val COMMAND_GAME_SPECTATOR = create("$COMMAND_PREFIX.spectator")
    val COMMAND_PLAYER = create("$COMMAND_PREFIX.player")

}