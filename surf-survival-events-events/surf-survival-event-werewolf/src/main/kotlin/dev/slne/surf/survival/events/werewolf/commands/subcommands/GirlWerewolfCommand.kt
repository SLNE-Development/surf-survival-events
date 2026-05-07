package dev.slne.surf.event.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuid
import dev.slne.surf.event.werewolf.service.WerewolfGameManager
import dev.slne.surf.event.werewolf.util.GameState
import dev.slne.surf.event.werewolf.util.GirlPeekOutcome
import dev.slne.surf.event.werewolf.util.NightStep
import dev.slne.surf.event.werewolf.util.WerwolfRoles
import net.kyori.adventure.text.format.TextDecoration

fun girlWerewolfCommand() = subcommand("girl") {
    playerExecutor { commandSender, _ ->
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

        if (service.engine.currentPhase != GameState.NIGHT) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst diesen Befehl nur während der Nacht benutzen!")
            }
            return@playerExecutor
        }

        if (service.getPlayerRole(commandSender.uuid()) != WerwolfRoles.GIRL) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur das Mädchen darf diesen Befehl benutzen.")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.GIRL) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Das Mädchen ist gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        when (val outcome = service.engine.peekWithGirl(commandSender.uuid())) {
            null -> commandSender.sendText {
                appendErrorPrefix()
                error("Deine Nachtaktion konnte nicht gespeichert werden.")
            }

            GirlPeekOutcome.TooDark -> commandSender.sendText {
                appendInfoPrefix()
                info("Es war zu dunkel.")
                appendNewInfoPrefixedLine()
                info("Du konntest heute Nacht nichts erkennen.")
            }

            is GirlPeekOutcome.FoundWerewolf -> {
                val werewolf = service.players[outcome.target]

                commandSender.sendText {
                    appendSuccessPrefix()
                    success("Du hast einen Werwolf gefunden!")
                    appendNewInfoPrefixedLine()
                    variableValue(werewolf?.name ?: "Unbekannt", TextDecoration.BOLD)
                    appendSpace()
                    append(WerwolfRoles.WERWOLF.displayName)
                }
            }

            GirlPeekOutcome.CaughtByWerewolves -> commandSender.sendText {
                appendErrorPrefix()
                error("Die Werwölfe haben dich erwischt!")
                appendNewInfoPrefixedLine()
                error("Du wirst heute Nacht sterben.")
            }
        }
    }
}
