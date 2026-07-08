package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.scoreboard.SurfScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService.Phase
import dev.slne.surf.survival.events.hideandseek.util.formatClock
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object HideAndSeekScoreboard {

    private val boards = ConcurrentHashMap<UUID, SurfScoreboard>()

    suspend fun show(player: Player) {
        val uuid = player.uniqueId
        if (boards.containsKey(uuid)) return

        val board = buildBoard(player)
        try {
            withContext(plugin.entityDispatcher(player)) {
                board.enable()
                board.addViewer(player)
            }
            boards[uuid] = board
        } catch (throwable: Throwable) {
            plugin.componentLogger.error("Failed to show hide and seek scoreboard for ${player.name}", throwable)
        }
    }

    suspend fun hide(uuid: UUID) {
        val board = boards.remove(uuid) ?: return
        val player = Bukkit.getPlayer(uuid)
        val dispatcher = if (player != null) plugin.entityDispatcher(player) else plugin.globalRegionDispatcher

        try {
            withContext(dispatcher) {
                board.disable()
            }
        } catch (throwable: Throwable) {
            plugin.componentLogger.error("Failed to hide hide and seek scoreboard", throwable)
        }
    }

    suspend fun hideAll() {
        boards.keys.toList().forEach { hide(it) }
    }

    private fun buildBoard(player: Player): SurfScoreboard =
        SurfScoreboardBuilder.builder(title())
            .maxLines(15)
            .addLineSeparator()
            .addEmptyLine()
            .addUpdatableLine { entry(Colors.PRIMARY, "Phase") { variableValue(phaseName()) } }
            .addUpdatableLine { entry(Colors.PRIMARY, "Zeit") { variableValue(formatClock(HideAndSeekService.remainingSeconds)) } }
            .addEmptyLine()
            .addUpdatableLine { roleLine(player) }
            .addEmptyLine()
            .addUpdatableLine { entry(HiderRole.color, "Verstecker") { variableValue(HideAndSeekRoleManager.onlineHiders.size) } }
            .addUpdatableLine { entry(SeekerRole.color, "Sucher") { variableValue(HideAndSeekRoleManager.onlineSeekers.size) } }
            .addUpdatableLine { entry(NamedTextColor.GRAY, "Spieler") { variableValue(onlineCount()) } }
            .addEmptyLine()
            .addLineSeparator()
            .buildAutoUpdatable()

    private fun title(): Component = buildText {
        variableValue("HIDE ", TextDecoration.BOLD)
        primary("& ", TextDecoration.BOLD)
        variableValue("SEEK", TextDecoration.BOLD)
    }

    private fun entry(
        dotColor: TextColor,
        label: String,
        value: SurfComponentBuilder.() -> Unit
    ): Component = buildText {
        append(Component.text("● ", dotColor))
        info(label)
        spacer("  ")
        value()
    }

    private fun roleLine(player: Player): Component {
        val role = HideAndSeekRoleManager.roleOf(player)
        return entry(role?.color ?: NamedTextColor.GRAY, "Rolle") {
            if (role != null) {
                append(Component.text(role.displayName, role.color, TextDecoration.BOLD))
            } else {
                variableValue("Lobby")
            }
        }
    }

    private fun onlineCount(): Int = GameService.snapshot()?.onlineEventPlayers?.size ?: 0

    private fun phaseName(): String = when (HideAndSeekService.phase) {
        Phase.IDLE -> "Warten"
        Phase.LOBBY -> "Lobby"
        Phase.PREPARATION -> "Verstecken"
        Phase.SEEKING -> "Suche"
        Phase.CELEBRATION -> "Ende"
    }
}
