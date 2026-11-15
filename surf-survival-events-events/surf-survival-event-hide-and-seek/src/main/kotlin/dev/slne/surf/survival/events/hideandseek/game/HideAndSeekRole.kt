package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.format.TextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player

sealed class HideAndSeekRole(
    val gameMode: GameMode = GameMode.ADVENTURE
) {
    /** Role name from the active theme. */
    val displayName: String get() = HideAndSeekThemes.roleTheme(this).name

    /** Role color from the active theme. */
    val color: TextColor get() = HideAndSeekThemes.color(HideAndSeekThemes.roleTheme(this).color)

    open suspend fun giveInventory(player: Player) {
        withContext(plugin.entityDispatcher(player)) {
            player.inventory.clear()
        }
    }

    open fun appliedScale(): Double = 1.0

    open fun canDamage(target: HideAndSeekRole): Boolean = false
}

object HiderRole : HideAndSeekRole() {
    override fun appliedScale() = HideAndSeekConfig.getConfig().gameplay.hiderScale
}

object SeekerRole : HideAndSeekRole() {
    override suspend fun giveInventory(player: Player) {
        HideAndSeekItems.giveSeekerKit(player)
    }

    override fun canDamage(target: HideAndSeekRole) = target == HiderRole
}

object SpectatorRole : HideAndSeekRole(GameMode.SPECTATOR)
