package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.service.RlglState
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.command.CommandSender

fun CommandAPICommand.statusCommand() = subcommand("status") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)

    anyExecutor { sender, _ ->
        showStatus(sender)
    }
}

private fun showStatus(sender: CommandSender) {
    val active = GameService.isActiveGame(RedLightGreenLightGame.KEY)
    val openRoundId = RlglService.currentOpenRoundId()
    val state = RlglService.getState()

    sender.sendText {
        appendInfoPrefix()
        primary("Red Light, Green Light Status")

        appendNewline()
        info("Session:")
        appendSpace()
        if (active) success("AKTIV") else error("INAKTIV")

        appendNewline()
        info("Runde:")
        appendSpace()
        when {
            openRoundId != null -> {
                variableValue("Offen")
                appendSpace()
                spacer("(ID:")
                appendSpace()
                variableValue(openRoundId)
                spacer(",")
                appendSpace()
                variableValue(RlglService.currentRoundMembers().size.toString())
                appendSpace()
                spacer("beigetreten)")
            }

            state == RlglState.GREEN || state == RlglState.RED -> {
                variableValue("Läuft")
                appendSpace()
                spacer("(Phase:")
                appendSpace()
                variableValue(if (state == RlglState.GREEN) "GRÜN" else "ROT")
                spacer(",")
                appendSpace()
                variableValue("${RlglService.aliveCount()}/${RlglService.totalRoundSize()}")
                appendSpace()
                spacer("aktiv)")
            }

            state == RlglState.FINISHED -> {
                variableValue("Beendet")
                appendSpace()
                spacer("(zuletzt ${RlglService.totalRoundSize()} Teilnehmer)")
            }

            else -> variableValue("Keine Runde")
        }

        appendNewline()
        info("Konfiguration:")
        RlglService.configChecks().forEach { check ->
            appendNewline()
            spacer("- ")
            variableValue(check.label)
            spacer(":")
            appendSpace()
            if (check.configured) success("OK") else error("NICHT GESETZT")
        }
    }
}