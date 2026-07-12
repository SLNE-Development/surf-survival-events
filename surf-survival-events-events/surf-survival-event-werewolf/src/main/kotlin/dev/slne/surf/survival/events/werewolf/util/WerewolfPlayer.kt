package dev.slne.surf.survival.events.werewolf.util

import dev.slne.surf.api.paper.extensions.server
import org.bukkit.GameMode
import java.util.*

data class WerewolfPlayer(
    val uuid: UUID,
    var role: WerwolfRoles = WerwolfRoles.VILLAGER,
    var isAlive: Boolean = true,
    var previousGameMode: GameMode? = null,
    var inLoveWith: UUID? = null,
    var hasPriestHolyWater: Boolean = true,
    var hasWitchHealPotion: Boolean = true,
    var hasWitchPoisonPotion: Boolean = true,
    val votes: MutableSet<UUID> = mutableSetOf(),
    var chosenVictim: UUID? = null
) {
    val name: String
        get() = server.getPlayer(uuid)?.name ?: "Unbekannt"
}
