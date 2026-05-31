package dev.slne.surf.survival.events.base.util

import dev.slne.surf.api.paper.permission.PermissionRegistry

public object PermissionRegistry : PermissionRegistry() {
    private const val PREFIX = "surf.survival.events"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    internal val COMMAND_ADMIN = create("$COMMAND_PREFIX.admin")
    internal val COMMAND_COMMUNITY_MANAGER = create("$COMMAND_PREFIX.community_manager")
    internal val COMMAND_GAME_SPECTATOR = create("$COMMAND_PREFIX.spectator")
    internal val COMMAND_PLAYER = create("$COMMAND_PREFIX.player")

}