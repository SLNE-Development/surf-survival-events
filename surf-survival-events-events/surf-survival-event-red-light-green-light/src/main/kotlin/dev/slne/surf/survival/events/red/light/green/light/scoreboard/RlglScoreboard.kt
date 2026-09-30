package dev.slne.surf.survival.events.red.light.green.light.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.service.RlglState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val activeScoreboards = ConcurrentHashMap<UUID, SurfAutoUpdatableScoreboard>()
private const val TOP_COUNT = 3
private const val SCOREBOARD_MAX_LINES = 15

fun Player.addToRlglScoreboard() {
    removeFromRlglScoreboard()

    val scoreboard = createRlglScoreboard(this)
    scoreboard.enable()
    scoreboard.addViewer(this)

    activeScoreboards[this.uniqueId] = scoreboard
}

fun Player.removeFromRlglScoreboard() {
    val scoreboard = activeScoreboards.remove(this.uniqueId) ?: return

    scoreboard.removeViewer(this)
    scoreboard.disable()
}

private fun createRlglScoreboard(selfPlayer: Player): SurfAutoUpdatableScoreboard {
    val builder = SurfScoreboardBuilder.builder(
        buildText {
            primary("Red Light, Green Light".toSmallCaps(), TextDecoration.BOLD)
        }
    ).maxLines(SCOREBOARD_MAX_LINES)

    builder.addUpdatableLine { phaseLine() }
    builder.addUpdatableLine { aliveLine() }
    builder.addUpdatableLine { selfStatusLine(selfPlayer) }
    builder.addEmptyLine()
    builder.addLine(topHeaderLine())

    repeat(TOP_COUNT) { index ->
        builder.addUpdatableLine { topLine(index) }
    }

    return builder.buildAutoUpdatable()
}

private fun phaseLine(): Component = buildText {
    primary("Phase:".toSmallCaps())
    appendSpace()
    when (RlglService.getState()) {
        RlglState.GREEN -> text("GRÜN", NamedTextColor.GREEN)
        RlglState.RED -> text("ROT", NamedTextColor.RED)
        RlglState.LOBBY -> info("Wartet")
        RlglState.FINISHED -> info("Beendet")
    }
}

private fun aliveLine(): Component = buildText {
    primary("Lebend:".toSmallCaps())
    appendSpace()
    variableValue(RlglService.aliveCount().toString())
    spacer("/")
    variableValue(RlglService.totalRoundSize().toString())
}

private fun selfStatusLine(selfPlayer: Player): Component {
    val uuid = selfPlayer.uniqueId
    val placement = RlglService.placementOf(uuid)

    return buildText {
        primary("Status:".toSmallCaps())
        appendSpace()
        when {
            placement != null -> variableValue("Platz $placement")
            RlglService.isActiveInRound(uuid) -> success("Im Rennen")
            RlglService.isRoundMember(uuid) -> error("Eliminiert")
            else -> info("Zuschauer")
        }
    }
}

private fun topHeaderLine(): Component = buildText {
    gold("Top $TOP_COUNT".toSmallCaps(), TextDecoration.BOLD)
}

private fun topLine(index: Int): Component {
    val uuid = RlglService.topFinishers(TOP_COUNT).getOrNull(index)
    val name = uuid?.let { Bukkit.getOfflinePlayer(it).name } ?: "-"

    return buildText {
        variableValue("#${index + 1}")
        appendSpace()
        info(name)
    }
}