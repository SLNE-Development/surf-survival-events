package dev.slne.surf.survival.events.red.light.green.light.utils

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionList : PermissionRegistry() {
    private const val PREFIX = "surf.survival.events.red_light_green_light"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    val COMMAND_COMMUNITY_MANAGER = create("$COMMAND_PREFIX.community_manager")
}