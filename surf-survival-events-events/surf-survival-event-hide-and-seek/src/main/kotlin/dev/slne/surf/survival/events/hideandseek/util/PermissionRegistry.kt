package dev.slne.surf.survival.events.hideandseek.util

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionRegistry : PermissionRegistry() {
    private const val PREFIX = "surf.survival.events.hideandseek"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    val COMMAND_COMMUNITY_MANAGER = create("$COMMAND_PREFIX.community_manager")
}
