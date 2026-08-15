package dev.slne.surf.survival.events.werewolf.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.CommonComponents.formatTime
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.survival.events.werewolf.inWerewolfColor
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val activeScoreboards = ConcurrentHashMap<UUID, SurfAutoUpdatableScoreboard>()
private const val SCOREBOARD_MAX_LINES = 15
private const val OVERVIEW_LINE_COUNT = 11

fun Player.addToWerewolfScoreboard() {
    removeFromWerewolfScoreboard()

    val scoreboard = createWerewolfScoreboard(this)
    scoreboard.enable()
    scoreboard.addViewer(this)

    activeScoreboards[this.uniqueId] = scoreboard
}

fun Player.removeFromWerewolfScoreboard() {
    val scoreboard = activeScoreboards.remove(this.uniqueId) ?: return

    scoreboard.removeViewer(this)
    scoreboard.disable()
}

fun updateWerewolfScoreboards() {
    activeScoreboards.values.forEach(SurfAutoUpdatableScoreboard::update)
}

private fun createWerewolfScoreboard(selfPlayer: Player): SurfAutoUpdatableScoreboard {
    val builder = SurfScoreboardBuilder.builder(
        buildText {
            inWerewolfColor("Werwolf".toSmallCaps(), TextDecoration.BOLD)
        }
    ).maxLines(SCOREBOARD_MAX_LINES)

    builder.addUpdatableLine { leaderLine(selfPlayer) }
    builder.addUpdatableLine { phaseLine(selfPlayer) }
    builder.addUpdatableLine { playerCountLine(selfPlayer) }
    builder.addUpdatableLine { roleOrStepLine(selfPlayer) }

    repeat(OVERVIEW_LINE_COUNT) { index ->
        builder.addUpdatableLine {
            overviewRows(selfPlayer).getOrElse(index) { Component.empty() }
        }
    }

    return builder.buildAutoUpdatable()
}

private fun leaderLine(selfPlayer: Player): Component {
    val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
    val partyLeader = game?.leader?.let { server.getPlayer(it)?.name } ?: "#Unbekannt"

    return buildText {
        primary("Erzähler:".toSmallCaps())
        appendSpace()
        variableValue(partyLeader)
    }
}

private fun phaseLine(selfPlayer: Player): Component {
    val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
    val timePlayed = game?.werewolfTime?.let {
        formatTime(
            it,
            showSeconds = true,
            shortForms = true,
            separator = buildText { spacer(":") }
        )
    } ?: text("00:00")

    return buildText {
        primary("Phase:".toSmallCaps())
        appendSpace()
        append(game?.state?.displayName ?: text("-"))
        appendSpace()
        spacer("|")
        appendSpace()
        append(timePlayed)
    }
}

private fun playerCountLine(selfPlayer: Player): Component {
    val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
    val aliveCount = game?.aliveCount ?: 0
    val totalCount = game?.totalCount ?: 0
    val deadCount = totalCount - aliveCount

    return buildText {
        primary("Lebend:".toSmallCaps())
        appendSpace()
        variableValue(aliveCount)
        appendSpace()
        spacer("|")
        appendSpace()
        primary("Tot:".toSmallCaps())
        appendSpace()
        variableValue(deadCount)
        spacer("/")
        variableValue(totalCount)
    }
}

private fun roleOrStepLine(selfPlayer: Player): Component {
    val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)

    return buildText {
        if (game?.isLeader(selfPlayer.uniqueId) == true) {
            primary("Nightstep:".toSmallCaps())
            appendSpace()
            variableValue(game.engine.currentNightStep?.let(::nightStepName) ?: "-")
        } else {
            primary("Rolle:".toSmallCaps())
            appendSpace()
            append(game?.getPlayerRole(selfPlayer.uniqueId)?.displayName ?: text("-"))
        }
    }
}

private fun overviewRows(selfPlayer: Player): List<Component> {
    val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
        ?: return listOf(buildText { info("Kein Werwolf-Spiel") })

    return if (game.isLeader(selfPlayer.uniqueId)) {
        leaderOverviewRows(game)
    } else {
        playerOverviewRows(game)
    }
}

private fun playerOverviewRows(game: WerewolfService): List<Component> {
    val alivePlayers = game.getAlivePlayers().sortedBy(WerewolfPlayer::name)
    if (alivePlayers.isEmpty()) return listOf(sectionLine("Lebende"), mutedLine("niemand"))

    val rows = mutableListOf<Component>()
    rows += sectionLine("Lebende")

    val visiblePlayers = alivePlayers.take(OVERVIEW_LINE_COUNT - 4)
    rows += visiblePlayers.map { playerLine(it, includeRole = false) }

    val hiddenCount = alivePlayers.size - visiblePlayers.size
    if (hiddenCount > 0) {
        rows += overviewHintLines()
    }

    return rows.take(OVERVIEW_LINE_COUNT)
}

private fun leaderOverviewRows(game: WerewolfService): List<Component> {
    val alivePlayers = game.getAlivePlayers().sortedForLeaderOverview()
    val deadPlayers = game.getDeadPlayers().sortedForLeaderOverview()
    val rows = mutableListOf<Component>()

    rows += sectionLine("Lebende")
    if (alivePlayers.isEmpty()) {
        rows += mutedLine("niemand")
    } else {
        val aliveLimit = if (deadPlayers.isEmpty()) OVERVIEW_LINE_COUNT - 4 else 4
        val visibleAlivePlayers = alivePlayers.take(aliveLimit)
        rows += visibleAlivePlayers.map { playerLine(it, includeRole = true) }

        val hiddenAliveCount = alivePlayers.size - visibleAlivePlayers.size
        if (hiddenAliveCount > 0) {
            rows += overviewHintLines()
        }
    }

    if (deadPlayers.isNotEmpty() && rows.size < OVERVIEW_LINE_COUNT) {
        rows += sectionLine("Tote")

        val remainingSlots = OVERVIEW_LINE_COUNT - rows.size
        val visibleDeadPlayers = deadPlayers.take((remainingSlots - 3).coerceAtLeast(0))
        rows += visibleDeadPlayers.map { playerLine(it, includeRole = true) }

        val hiddenDeadCount = deadPlayers.size - visibleDeadPlayers.size
        if (hiddenDeadCount > 0 && rows.size < OVERVIEW_LINE_COUNT) {
            rows += overviewHintLines()
        }
    }

    return rows.take(OVERVIEW_LINE_COUNT)
}

private fun List<WerewolfPlayer>.sortedForLeaderOverview(): List<WerewolfPlayer> =
    sortedWith(compareBy<WerewolfPlayer> { it.role.ordinal }.thenBy { it.name })

private fun sectionLine(label: String): Component = buildText {
    gold(label.toSmallCaps(), TextDecoration.BOLD)
}

private fun playerLine(player: WerewolfPlayer, includeRole: Boolean): Component = buildText {
    variableValue(player.name)

    if (includeRole) {
        appendSpace()
        spacer("-")
        appendSpace()
        append(player.role.displayName)
    }
}

private fun overviewHintLines(): List<Component> = listOf(
    buildText { info("Benutze") },
    buildText { variableValue("/werewolf overview") },
    buildText { info("um alle zusehen.") }
)

private fun mutedLine(text: String): Component = buildText {
    info(text)
}

private fun nightStepName(step: NightStep): String = when (step) {
    NightStep.AMOR -> "Amor"
    NightStep.WEREWOLVES -> "Werwölfe"
    NightStep.GIRL -> "Mädchen"
    NightStep.SEER -> "Seherin"
    NightStep.DOCTOR -> "Doktor"
    NightStep.WITCH -> "Hexe"
    NightStep.SERIAL_KILLER -> "Serienmörder"
    NightStep.RESOLVE -> "Auflösung"
}
