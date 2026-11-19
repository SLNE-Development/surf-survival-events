package dev.slne.surf.survival.events.hideandseek.game

import dev.slne.surf.api.paper.sidebar.SurfAutoUpdatingSidebar
import dev.slne.surf.api.paper.sidebar.autoUpdatingSidebar
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService.Phase
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekTheme.ScoreboardLine
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
import dev.slne.surf.survival.events.hideandseek.util.formatClock
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * The hide and seek sidebar. One sidebar is shared by all viewers; only the role line is rendered
 * per viewer.
 */
object HideAndSeekScoreboard {

    private val viewers: MutableSet<UUID> = Collections.synchronizedSet(HashSet())

    @Volatile
    private var sidebar: SurfAutoUpdatingSidebar? = null

    suspend fun show(player: Player) {
        viewers.add(player.uniqueId)
        try {
            currentSidebar().addViewer(player)
        } catch (throwable: Throwable) {
            viewers.remove(player.uniqueId)
            plugin.componentLogger.error("Failed to show hide and seek scoreboard for ${player.name}", throwable)
        }
    }

    suspend fun hide(uuid: UUID) {
        if (!viewers.remove(uuid)) return

        val player = Bukkit.getPlayer(uuid) ?: return
        try {
            sidebar?.removeViewer(player)
        } catch (throwable: Throwable) {
            plugin.componentLogger.error("Failed to hide hide and seek scoreboard", throwable)
        }
    }

    suspend fun hideAll() {
        viewers.clear()
        closeSidebar()
    }

    /** Rebuilds the sidebar for every viewer, so a newly loaded theme shows up immediately. */
    suspend fun rebuildAll() {
        val current = viewers.toList()
        closeSidebar()
        current.forEach { uuid -> Bukkit.getPlayer(uuid)?.let { show(it) } }
    }

    @Synchronized
    private fun currentSidebar(): SurfAutoUpdatingSidebar {
        return sidebar?.takeUnless { it.isClosed } ?: buildSidebar().also { sidebar = it }
    }

    @Synchronized
    private fun closeSidebar() {
        sidebar?.close()
        sidebar = null
    }

    private fun buildSidebar(): SurfAutoUpdatingSidebar {
        val scoreboard = HideAndSeekThemes.theme.scoreboard

        return autoUpdatingSidebar(plugin) {
            title(scoreboard.title)
            updateInterval = 1.seconds

            separator()
            emptyLine()
            sharedLine { entry(theme().phase, accent()) { value(phaseName()) } }
            sharedLine { entry(theme().time, accent()) { value(formatClock(HideAndSeekService.remainingSeconds)) } }
            emptyLine()
            line { viewer -> roleLine(viewer) }
            emptyLine()
            sharedLine { entry(theme().hiders, HiderRole.color) { value(HideAndSeekRoleManager.onlineHiders.size) } }
            sharedLine { entry(theme().seekers, SeekerRole.color) { value(HideAndSeekRoleManager.onlineSeekers.size) } }
            sharedLine { entry(theme().players, NamedTextColor.GRAY) { value(onlineCount()) } }
            emptyLine()

            // The board holds 15 lines, which leaves room for two footer lines.
            scoreboard.footer.take(2).forEach { line(it) }
            if (scoreboard.footer.isNotEmpty()) {
                emptyLine()
            }

            separator()
        }
    }

    private fun theme() = HideAndSeekThemes.theme.scoreboard

    private fun accent(): TextColor = theme().accentColor

    private fun value(value: Any): Component =
        Component.text(value.toString(), HideAndSeekThemes.theme.valueColor)

    private fun entry(line: ScoreboardLine, iconColor: TextColor, value: () -> Component): Component {
        val icon = line.icon.colorIfAbsent(iconColor)
        val label = line.label.colorIfAbsent(theme().labelColor)

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
