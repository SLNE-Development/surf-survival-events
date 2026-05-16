package dev.slne.surf.survival.events.werewolf.scoreboard

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.CommonComponents.formatTime
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatableScoreboard
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.inWerewolfColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import java.util.*

private val activeScoreboards = mutableMapOf<UUID, SurfAutoUpdatableScoreboard>()

fun Player.addToWerewolfScoreboard() {
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

private fun createWerewolfScoreboard(selfPlayer: Player) =
    SurfScoreboardBuilder.builder(
        buildText {
            inWerewolfColor("Community Werwolf".toSmallCaps(), TextDecoration.BOLD)
        }
    )
        .addUpdatableLine {
            val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
            val partyLeader = game?.leader?.let { server.getPlayer(it)?.name } ?: "#Unbekannt"
            buildText {
                primary("Erzähler:".toSmallCaps())
                appendSpace()
                variableValue(partyLeader)

            }
        }

        .addUpdatableLine {
            val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
            val aliveCount = game?.aliveCount ?: 0
            val totalCount = game?.totalCount ?: 0
            buildText {
                primary("Lebende Spieler:".toSmallCaps())
                appendSpace()
                variableValue(aliveCount)
                appendSpace()
                spacer("/")
                variableValue(totalCount)
            }
        }
        .addEmptyLine()

        .addUpdatableLine {
            val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
            val timePlayed = game?.werewolfTime?.let { formatTime(
                it,
                showSeconds = true,
                shortForms = true,
                separator = buildText { spacer(":") }
            ) } ?: text("00:00")


            buildText {
                primary("Spielzeit:".toSmallCaps())
                appendSpace()
                append(timePlayed)
                appendSpace()
            }
        }
        .addEmptyLine()

        .addUpdatableLine {
            val game = WerewolfGameManager.getGameForPlayer(selfPlayer.uniqueId)
            val roleDisplay = game?.getPlayerRole(selfPlayer.uniqueId)?.displayName ?: text("Unbekannte Rolle")
            buildText {
                primary("Deine Spielrolle:".toSmallCaps())
                appendSpace()
                append(roleDisplay)
            }
        }
        .addEmptyLine()
        .buildAutoUpdatable()