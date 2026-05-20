package dev.slne.surf.survival.events.werewolf.permissions

import dev.slne.surf.api.paper.extensions.pluginManager
import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionRegistry : PermissionRegistry() {
    private const val PREFIX = "surf.survival.events.werewolf"
    private const val COMMAND_PREFIX = "$PREFIX.command"

    val COMMAND_WEREWOLF_USE = create("$COMMAND_PREFIX.use")
    val COMMAND_WEREWOLF_ADMIN = create("$COMMAND_PREFIX.admin")

    init {
        pluginManager.getPermission(COMMAND_WEREWOLF_USE)?.addParent(COMMAND_WEREWOLF_ADMIN, true)
    }
}
