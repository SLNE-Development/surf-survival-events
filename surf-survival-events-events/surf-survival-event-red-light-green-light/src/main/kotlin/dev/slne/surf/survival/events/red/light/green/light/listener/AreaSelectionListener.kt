package dev.slne.surf.survival.events.red.light.green.light.listener

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.event.cancel
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaSelection
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaSelectionActionBar
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaSelectionSounds
import dev.slne.surf.survival.events.red.light.green.light.command.util.AreaTarget
import dev.slne.surf.survival.events.red.light.green.light.config.RlglConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.BoundingBox
import java.util.UUID
import kotlin.math.abs

object AreaSelectionListener : Listener {

    @EventHandler
    fun onLeftClickBlock(event: PlayerInteractEvent) {
        if (event.action != Action.LEFT_CLICK_BLOCK) return

        val player = event.player
        val target = AreaSelection.getActiveTarget(player.uniqueId) ?: return

        event.cancel()
        val block = event.clickedBlock ?: return
        val location = block.location

        val selection = AreaSelection.offerBlock(player.uniqueId, location)
        if (selection == null) {
            AreaSelectionSounds.playPositionSet(player)

            player.sendText {
                appendSuccessPrefix()
                success("Pos1:")
                appendSpace()
                variableValue(location.readableString(true))
            }
            return
        }

        val (pos1, pos2) = selection
        val world = block.world
        val box = BoundingBox(
            pos1.x, world.minHeight.toDouble(), pos1.z,
            pos2.x, world.maxHeight.toDouble(), pos2.z
        )

        RlglConfig.edit {
            when (target) {
                AreaTarget.START -> start = box
                AreaTarget.FINISH -> finish = box
            }
        }

        AreaSelectionSounds.playAreaSaved(player)

        val widthX = abs(pos2.x - pos1.x).toInt() + 1
        val widthZ = abs(pos2.z - pos1.z).toInt() + 1

        player.sendText {
            appendSuccessPrefix()
            success("${target.displayName}-Area zwischen")
            appendSpace()
            variableValue(pos1.readableString(true))
            appendSpace()
            success("und")
            appendSpace()
            variableValue(pos2.readableString(true))
            appendSpace()
            success("gespeichert (volle Welthöhe).")
            appendNewline()
            info("Fläche:")
            appendSpace()
            variableValue("${widthX}x$widthZ")
            appendSpace()
            info("Blöcke")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) = disable(event.player.uniqueId)

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) = disable(event.player.uniqueId)

    private fun disable(uuid: UUID) {
        AreaSelection.disable(uuid)
        AreaSelectionActionBar.stop(uuid)
    }
}