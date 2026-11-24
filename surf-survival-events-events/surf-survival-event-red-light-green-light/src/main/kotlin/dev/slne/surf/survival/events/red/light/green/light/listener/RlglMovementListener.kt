package dev.slne.surf.survival.events.red.light.green.light.listener

import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

object RlglMovementListener : Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to

        if (from.x == to.x && from.y == to.y && from.z == to.z) return
        if (!GameService.isActiveGame(RedLightGreenLightGame.KEY)) return

        GameService.withGameContext(RedLightGreenLightGame.KEY) {
            if (RlglService.shouldCancelCountdownMovement(event.player, to)) {
                event.isCancelled = true
                return@withGameContext
            }

            RlglService.handleMovement(event.player)
        }
    }
}