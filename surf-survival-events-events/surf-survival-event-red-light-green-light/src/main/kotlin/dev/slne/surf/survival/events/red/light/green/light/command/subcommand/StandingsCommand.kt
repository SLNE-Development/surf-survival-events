package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.pagination.Pagination
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.service.RoundStanding
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.entity.Player

fun CommandAPICommand.standingsCommand() = subcommand("standings") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)

    subcommand("finished") {
        integerArgument("page", optional = true)

        playerExecutor { player, arguments ->
            val page: Int? by arguments
            showStandings(player, page ?: 1, finished = true)
        }
    }

    subcommand("unfinished") {
        integerArgument("page", optional = true)

        playerExecutor { player, arguments ->
            val page: Int? by arguments
            showStandings(player, page ?: 1, finished = false)
        }
    }
}

private fun showStandings(player: Player, page: Int, finished: Boolean) {
    val standings = RlglService.lastRoundStandings()

    if (standings.isEmpty()) {
        player.sendText {
            appendErrorPrefix()
            error("Es liegen noch keine Ergebnisse einer beendeten Runde vor.")
        }
        return
    }

    val filtered = standings
        .filter { it.finished == finished }
        .let { list -> if (finished) list.sortedBy { it.placement } else list.sortedBy { it.name } }

    if (filtered.isEmpty()) {
        player.sendText {
            appendInfoPrefix()
            info(
                if (finished) "Niemand hat das Ziel erreicht."
                else "Alle Spieler haben das Ziel erreicht."
            )
        }
        return
    }

    val pagination = Pagination<RoundStanding> {
        title {
            primary(if (finished) "Ins Ziel gelaufen" else "Nicht geschafft")
            spacer(" (${filtered.size})")
        }

        rowRendererSimple { standing ->
            buildText {
                if (finished) {
                    variableValue("#${standing.placement}")
                    appendSpace()
                    variableValue(standing.name)
                    appendSpace()
                    spacer("(")
                    variableValue(formatDuration(standing.finishTimeMillis ?: 0L))
                    spacer(")")
                } else {
                    variableValue(standing.name)
                }
            }
        }
    }

    player.sendText {
        append(pagination.renderComponent(filtered, page))
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (millis % 1000) / 100
    return if (minutes > 0) {
        "%d:%02d.%d".format(minutes, seconds, tenths)
    } else {
        "%d.%ds".format(seconds, tenths)
    }
}