package dev.slne.surf.survival.events.hideandseek.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.game.PlayerRemoveReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.game.*
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekBroadcast.broadcast
import dev.slne.surf.survival.events.hideandseek.util.tp
import kotlinx.coroutines.withContext
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt
import org.bukkit.Sound as BukkitSound

object HideAndSeekRoleManager {

    private val roles = ConcurrentHashMap<UUID, HideAndSeekRole>()

    private val bypassingPlayers = ConcurrentHashMap.newKeySet<UUID>()

    fun roleOf(uuid: UUID): HideAndSeekRole? = roles[uuid]
    fun roleOf(player: Player): HideAndSeekRole? = roleOf(player.uniqueId)

    fun isBypassing(player: Player): Boolean = player.uniqueId in bypassingPlayers

    fun switchBypass(player: Player): Boolean {
        return if (bypassingPlayers.remove(player.uniqueId)) {
            false
        } else {
            bypassingPlayers.add(player.uniqueId)
            true
        }
    }

    val onlineSeekers: List<Player> get() = onlinePlayersWithRole(SeekerRole)
    val onlineHiders: List<Player> get() = onlinePlayersWithRole(HiderRole)

    private fun onlinePlayersWithRole(role: HideAndSeekRole): List<Player> {
        return roles.filterValues { it == role }.keys.mapNotNull(Bukkit::getPlayer)
    }

    fun clearRoles() = roles.clear()

    fun removeRole(uuid: UUID) {
        roles.remove(uuid)
    }

    suspend fun setRole(player: Player, role: HideAndSeekRole, announce: Boolean = true) {
        roles[player.uniqueId] = role
        withContext(plugin.entityDispatcher(player)) {
            player.gameMode = role.gameMode
            applyScale(player, role.appliedScale())
        }
        role.giveInventory(player)

        if (announce) {
            player.sendText {
                appendInfoPrefix()
                info("Du bist jetzt ein ")
                text(role.displayName, role.color, TextDecoration.BOLD)
                info("!")
            }
            player.playSound {
                type(BukkitSound.ENTITY_PLAYER_LEVELUP)
                volume(.75f)
                source(Sound.Source.PLAYER)
            }
        }
    }

    suspend fun onHiderCaught(player: Player) {
        if (roleOf(player) != HiderRole) return
        if (!HideAndSeekService.isRolePhase) return

        if (HideAndSeekConfig.getConfig().gameplay.hidersBecomeSeekers) {
            setRole(player, SeekerRole)
            HideAndSeekService.performPlayerCheck()
        } else {
            GameService.remove(player.uniqueId, includeSpectators = false, reason = PlayerRemoveReason.KICK)
        }
    }

    suspend fun handlePostRespawn(player: Player) {
        val role = roleOf(player) ?: return

        if (role == HiderRole && HideAndSeekService.isSeekingPhase) {
            onHiderCaught(player)
            return
        }

        role.giveInventory(player)

        if (role == SpectatorRole) {
            withContext(plugin.entityDispatcher(player)) {
                player.gameMode = GameMode.SPECTATOR
            }
            currentContext()?.let { context ->
                player.tp(HideAndSeekConfig.getConfig().spectatorSpawn.toLocation(context))
            }
        }
    }

    suspend fun handleParticipantRemove(uuid: UUID, player: Player?, disconnected: Boolean) {
        roles.remove(uuid)
        HideAndSeekScoreboard.hide(uuid)

        if (player != null && !disconnected) {
            resetPlayer(player)
            GameService.teleportToServerLobby(player)
        }

        HideAndSeekService.performPlayerCheck()
    }

    fun announceDeath(player: Player, role: HideAndSeekRole) {
        val killer = player.killer
        val killerRole = killer?.let { roleOf(it) }
        val remainingHiders = (onlineHiders.size - if (role == HiderRole) 1 else 0)
            .coerceAtLeast(0)

        broadcast {
            appendInfoPrefix()
            coloredComponent(player.name, role.color)
            if (killer != null && killerRole != null) {
                info(" wurde von ")
                coloredComponent(killer.name, killerRole.color)
                info(" gefangen!")
            } else {
                info(" ist ausgeschieden!")
            }

            if (role == HiderRole && remainingHiders > 0) {
                appendSpace()
                info("Noch ")
                variableValue(remainingHiders)
                info(" Verstecker übrig.")
            }
        }
    }

    suspend fun setupLobbyPlayer(player: Player, context: GameContext) {
        roles.remove(player.uniqueId)
        resetPlayer(player)
        player.tp(HideAndSeekConfig.getConfig().lobbySpawn.toLocation(context))
        HideAndSeekScoreboard.show(player)
    }

    suspend fun setupSpectator(player: Player, context: GameContext) {
        roles[player.uniqueId] = SpectatorRole
        resetPlayer(player)
        withContext(plugin.entityDispatcher(player)) {
            player.gameMode = GameMode.SPECTATOR
        }
        player.tp(HideAndSeekConfig.getConfig().spectatorSpawn.toLocation(context))
        HideAndSeekScoreboard.show(player)
    }

    suspend fun resetPlayer(player: Player) {
        withContext(plugin.entityDispatcher(player)) {
            with(player) {
                inventory.clear()
                health = getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
                foodLevel = 20
                saturation = 20f
                fireTicks = 0
                isFlying = false
                allowFlight = false
                gameMode = GameMode.ADVENTURE
            }
            applyScale(player, 1.0)
        }
    }

    internal fun applyScale(player: Player, scale: Double) {
        player.getAttribute(Attribute.SCALE)?.baseValue = scale
        player.walkSpeed = (0.2 * sqrt(scale)).coerceIn(0.01, 1.0).toFloat()
    }
}
