package dev.slne.surf.survival.events.freebuild.permission

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionList : PermissionRegistry() {
    private const val BASE = "surf.survival.events.freebuild"

    val COMMAND_SWITCH = create("$BASE.switch")
}