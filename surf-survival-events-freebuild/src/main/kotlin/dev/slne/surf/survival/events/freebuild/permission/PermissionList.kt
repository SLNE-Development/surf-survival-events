package dev.slne.surf.survival.events.freebuild.permission

import dev.slne.surf.api.paper.permission.PermissionRegistry

object PermissionList : PermissionRegistry() {
    private const val BASE = "surf.survival.events.freebuild"

    val COMMAND_SWITCH = create("$BASE.switch")
    val COMMAND_RELOAD = create("$BASE.reload")
    val COMMAND_JOIN = create("$BASE.join")
    val COMMAND_CERTIFICATE = create("$BASE.certificate")
    val COMMAND_CERTIFICATE_COPY = create("$BASE.certificate.copy")
}