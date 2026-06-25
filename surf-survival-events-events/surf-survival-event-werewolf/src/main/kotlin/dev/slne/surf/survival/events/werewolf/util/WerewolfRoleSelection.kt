package dev.slne.surf.survival.events.werewolf.util

import java.util.*

object WerewolfRoleSelection {

    fun assignRoles(participants: List<UUID>): Map<UUID, WerwolfRoles> {
        val uuids = participants.shuffled()
        val count = uuids.size

        val roles = when {
            count >= 15 -> getRolesFor15()
            count >= 12 -> getRolesFor12()
            count >= 10 -> getRolesFor10()
            else -> getRolesFor8()
        }.toMutableList()

        while (roles.size < count) {
            roles.add(WerwolfRoles.VILLAGER)
        }

        roles.shuffle()

        return uuids.mapIndexed { index, uuid ->
            uuid to roles[index]
        }.toMap()
    }

    private fun getRolesFor8() = listOf(
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.SEER,
        WerwolfRoles.WITCH,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER
    )

    private fun getRolesFor10() = listOf(
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.SEER,
        WerwolfRoles.WITCH,
        WerwolfRoles.DOCTOR,
        WerwolfRoles.AMOR,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER
    )

    private fun getRolesFor12() = listOf(
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.SEER,
        WerwolfRoles.WITCH,
        WerwolfRoles.DOCTOR,
        WerwolfRoles.AMOR,
        WerwolfRoles.GIRL,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER
    )

    private fun getRolesFor15() = listOf(
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.WERWOLF,
        WerwolfRoles.SEER,
        WerwolfRoles.WITCH,
        WerwolfRoles.DOCTOR,
        WerwolfRoles.AMOR,
        WerwolfRoles.GIRL,
        WerwolfRoles.PRIEST,
        WerwolfRoles.SERIAL_KILLER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER,
        WerwolfRoles.VILLAGER
    )
}
