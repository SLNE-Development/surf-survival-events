package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuid
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

fun voteWerewolfCommand() = subcommand("vote") {
    withRequirement { sender -> WerewolfCommandRequirements.canVote(sender) }
    withArguments(EntitySelectorArgument.OnePlayer("player"))
    playerExecutor { commandSender, arguments ->
        val targetPlayer = arguments.get("player") as Player
        val service = WerewolfGameManager.getGameForPlayer(commandSender.uuid())

        if (service == null) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du bist aktuell in keinem Werwolf-Spiel.")
            }
            return@playerExecutor
        }

        if (service.isPhaseTransitioning) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Der Phasenwechsel läuft gerade noch. Warte einen kurzen Moment.")
            }
            return@playerExecutor
        }

        val currentPhase = service.engine.currentPhase
        if (currentPhase != GameState.MAYOR_VOTE && currentPhase != GameState.VOTE) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst diesen Befehl nur während einer Voting-Phase benutzen!")
            }
            return@playerExecutor
        }

        when (currentPhase) {
            GameState.MAYOR_VOTE -> {

                if (!service.engine.submitMayorVote(commandSender.uuid(), targetPlayer.uuid())) {
                    commandSender.sendText {
                        appendErrorPrefix()
                        error("Die Bürgermeisterwahl ist aktuell nicht verfügbar oder das Ziel ist ungültig.")
                    }
                    return@playerExecutor
                }

                commandSender.sendText {
                    appendSuccessPrefix()
                    success("Du hast erfolgreich für")
                    appendSpace()
                    variableValue(targetPlayer.name, TextDecoration.BOLD)
                    appendSpace()
                    success("abgestimmt!")
                }
            }

            GameState.VOTE -> {

                if (!service.engine.submitVote(commandSender.uuid(), targetPlayer.uuid())) {
                    commandSender.sendText {
                        appendErrorPrefix()
                        error("Die Abstimmung ist aktuell nicht verfügbar oder das Ziel ist ungültig.")
                    }
                    return@playerExecutor
                }

                commandSender.sendText {
                    appendSuccessPrefix()
                    success("Du hast erfolgreich für")
                    appendSpace()
                    variableValue(targetPlayer.name)
                    appendSpace()
                    success("abgestimmt!")
                }
            }
        }
    }
}
