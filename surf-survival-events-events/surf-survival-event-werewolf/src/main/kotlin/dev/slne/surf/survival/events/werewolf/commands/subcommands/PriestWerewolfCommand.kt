package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.PriestActionResult
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import org.bukkit.entity.Player

fun priestWerewolfCommand() = subcommand("priest") {
    withRequirement { sender -> WerewolfCommandRequirements.canActAsPriest(sender) }
    entitySelectorArgumentOnePlayer("targetPlayer")

    playerExecutor { commandSender, arguments ->
        val targetPlayer: Player by arguments
        val service = WerewolfGameManager.getGameForPlayer(commandSender.uniqueId)

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

        if (service.engine.currentPhase != GameState.DAY) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst Weihwasser nur während des Tages werfen.")
            }
            return@playerExecutor
        }

        if (service.getPlayerRole(commandSender.uniqueId) != WerwolfRoles.PRIEST) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur der Priester darf diesen Befehl benutzen.")
            }
            return@playerExecutor
        }

        when (val result = service.engine.usePriestHolyWater(commandSender.uniqueId, targetPlayer.uniqueId)) {
            PriestActionResult.WrongPhase -> commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst Weihwasser nur während des Tages werfen.")
            }

            PriestActionResult.InvalidActor -> commandSender.sendText {
                appendErrorPrefix()
                error("Deine Priester-Aktion konnte nicht ausgeführt werden.")
            }

            PriestActionResult.InvalidTarget -> commandSender.sendText {
                appendErrorPrefix()
                error("Du musst einen anderen lebenden Spieler auswählen.")
            }

            PriestActionResult.AlreadyUsed -> commandSender.sendText {
                appendErrorPrefix()
                error("Du hast dein Weihwasser bereits benutzt.")
            }

            is PriestActionResult.Success -> {
                commandSender.sendText {
                    appendSuccessPrefix()
                    success("Dein Weihwasser wurde geworfen.")
                }

                result.winner?.let(service::finishGame)
            }
        }
    }
}
