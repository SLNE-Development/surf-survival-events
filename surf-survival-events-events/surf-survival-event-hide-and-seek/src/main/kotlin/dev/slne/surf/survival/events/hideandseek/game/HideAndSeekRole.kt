package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.plugin
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.format.TextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player

sealed class HideAndSeekRole(
    val displayName: String,
    val color: TextColor,
    val gameMode: GameMode = GameMode.ADVENTURE
) {
    open suspend fun giveInventory(player: Player) {
        withContext(plugin.entityDispatcher(player)) {
            player.inventory.clear()
        }
    }

    open fun appliedScale(): Double = 1.0

    open fun canDamage(target: HideAndSeekRole): Boolean = false
}

object HiderRole : HideAndSeekRole("Verstecker", TextColor.color(0x3498DB)) {
    override fun appliedScale() = HideAndSeekConfig.getConfig().gameplay.hiderScale
}

object SeekerRole : HideAndSeekRole("Sucher", TextColor.color(0xE74C3C)) {
    override suspend fun giveInventory(player: Player) {
        HideAndSeekItems.giveSeekerKit(player)
    }

    override fun canDamage(target: HideAndSeekRole) = target == HiderRole
}

object SpectatorRole : HideAndSeekRole("Zuschauer", TextColor.color(0x9B59B6), GameMode.SPECTATOR)
