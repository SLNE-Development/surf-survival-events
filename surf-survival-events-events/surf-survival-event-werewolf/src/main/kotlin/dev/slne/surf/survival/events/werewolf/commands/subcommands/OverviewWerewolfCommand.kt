package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import org.bukkit.entity.Player

fun overviewWerewolfCommand() = subcommand("overview") {
    withRequirement { sender -> WerewolfCommandRequirements.canViewOverview(sender) }

    playerExecutor { player, _ ->
        val service = WerewolfGameManager.getGameForPlayer(player.uniqueId)

        if (service == null) {
            player.sendText {
                appendErrorPrefix()
                error("Du bist aktuell in keinem Werwolf-Spiel.")
            }
            return@playerExecutor
        }

        if (service.isLeader(player.uniqueId)) {
            sendLeaderOverview(player, service)
        } else {
            sendPlayerOverview(player, service)
        }
    }
}

private fun sendPlayerOverview(player: Player, service: WerewolfService) {
    val alivePlayers = service.getAlivePlayers().sortedBy(WerewolfPlayer::name)
    val deadCount = service.getDeadPlayers().size

    player.sendText {
        appendInfoPrefix()
        variableValue("Werwolf Übersicht")

        appendNewInfoPrefixedLine()
        primary("Lebende Spieler:")
        appendSpace()
        variableValue(alivePlayers.size)

        if (alivePlayers.isEmpty()) {
            appendNewInfoPrefixedLine()
            info("niemand")
        } else {
            alivePlayers.forEach { werewolfPlayer ->
                appendNewInfoPrefixedLine()
                variableValue(werewolfPlayer.name)
            }
        }

        appendNewInfoPrefixedLine()
        primary("Tote:")
        appendSpace()
        variableValue(deadCount)
        spacer("/")
        variableValue(service.totalCount)
    }
}

private fun sendLeaderOverview(player: Player, service: WerewolfService) {
    val alivePlayers = service.getAlivePlayers().sortedForOverview()
    val deadPlayers = service.getDeadPlayers().sortedForOverview()

    player.sendText {
        appendInfoPrefix()
        variableValue("Werwolf Übersicht")

        appendNewInfoPrefixedLine()
        primary("Lebende:")
        appendSpace()
        variableValue(alivePlayers.size)

        if (alivePlayers.isEmpty()) {
            appendNewInfoPrefixedLine()
            info("niemand")
        } else {
            alivePlayers.forEach { werewolfPlayer ->
                appendNewInfoPrefixedLine()
                variableValue(werewolfPlayer.name)
                appendSpace()
                spacer("-")
                appendSpace()
                append(werewolfPlayer.role.displayName)
            }
        }

        appendNewInfoPrefixedLine()
        primary("Tote:")
        appendSpace()
        variableValue(deadPlayers.size)
        spacer("/")
        variableValue(service.totalCount)

        if (deadPlayers.isEmpty()) {
            appendNewInfoPrefixedLine()
            info("niemand")
        } else {
            deadPlayers.forEach { werewolfPlayer ->
                appendNewInfoPrefixedLine()
                variableValue(werewolfPlayer.name)
                appendSpace()
                spacer("-")
                appendSpace()
                append(werewolfPlayer.role.displayName)
            }
        }
    }
}

private fun List<WerewolfPlayer>.sortedForOverview(): List<WerewolfPlayer> =
    sortedWith(compareBy<WerewolfPlayer> { it.role.ordinal }.thenBy { it.name })
