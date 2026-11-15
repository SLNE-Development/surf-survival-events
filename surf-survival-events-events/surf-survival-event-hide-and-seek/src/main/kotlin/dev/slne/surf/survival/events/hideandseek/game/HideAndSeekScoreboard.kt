package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.api.paper.scoreboard.SurfScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService.Phase
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekTheme.ScoreboardLine
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
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

    /** Rebuilds every visible scoreboard, so a newly loaded theme shows up immediately. */
    suspend fun rebuildAll() {
        boards.keys.toList().forEach { uuid ->
            hide(uuid)
            Bukkit.getPlayer(uuid)?.let { show(it) }
        }
    }

    private fun buildBoard(player: Player): SurfScoreboard {
        val scoreboard = HideAndSeekThemes.theme.scoreboard

        var builder = SurfScoreboardBuilder.builder(HideAndSeekThemes.render(scoreboard.title))
            .maxLines(15)
            .addLineSeparator()
            .addEmptyLine()
            .addUpdatableLine { entry(theme().phase, accent()) { value(phaseName()) } }
            .addUpdatableLine { entry(theme().time, accent()) { value(formatClock(HideAndSeekService.remainingSeconds)) } }
            .addEmptyLine()
            .addUpdatableLine { roleLine(player) }
            .addEmptyLine()
            .addUpdatableLine { entry(theme().hiders, HiderRole.color) { value(HideAndSeekRoleManager.onlineHiders.size) } }
            .addUpdatableLine { entry(theme().seekers, SeekerRole.color) { value(HideAndSeekRoleManager.onlineSeekers.size) } }
            .addUpdatableLine { entry(theme().players, NamedTextColor.GRAY) { value(onlineCount()) } }
            .addEmptyLine()

        // The board holds 15 lines, which leaves room for two footer lines.
        scoreboard.footer.take(2).forEach { line ->
            builder = builder.addLine(HideAndSeekThemes.render(line))
        }
        if (scoreboard.footer.isNotEmpty()) {
            builder = builder.addEmptyLine()
        }

        return builder
            .addLineSeparator()
            .buildAutoUpdatable()
    }

    private fun theme() = HideAndSeekThemes.theme.scoreboard

    private fun accent(): TextColor = HideAndSeekThemes.color(theme().accentColor)

    private fun value(value: Any): Component =
        Component.text(value.toString(), HideAndSeekThemes.color(HideAndSeekThemes.theme.valueColor))

    private fun entry(line: ScoreboardLine, iconColor: TextColor, value: () -> Component): Component {
        val icon = HideAndSeekThemes.render(line.icon).colorIfAbsent(iconColor)
        val label = HideAndSeekThemes.render(line.label).colorIfAbsent(HideAndSeekThemes.color(theme().labelColor))

        return Component.text()
            .append(icon)
            .append(Component.space())
            .append(label)
            .append(Component.text("  ", NamedTextColor.GRAY))
            .append(value())
            .build()
    }

    private fun roleLine(player: Player): Component {
        val role = HideAndSeekRoleManager.roleOf(player)
        return entry(theme().role, role?.color ?: NamedTextColor.GRAY) {
            if (role != null) {
                Component.text(role.displayName, role.color, TextDecoration.BOLD)
            } else {
                value(HideAndSeekThemes.theme.roles.noRole)
            }
        }
    }

    private fun onlineCount(): Int = GameService.snapshot()?.onlineEventPlayers?.size ?: 0

    private fun phaseName(): String = with(HideAndSeekThemes.theme.phases) {
        when (HideAndSeekService.phase) {
            Phase.IDLE -> idle
            Phase.LOBBY -> lobby
            Phase.PREPARATION -> hiding
            Phase.SEEKING -> seeking
            Phase.CELEBRATION -> celebration
        }
    }
}
