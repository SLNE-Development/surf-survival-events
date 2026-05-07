package dev.slne.surf.event.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuid
import dev.slne.surf.event.werewolf.service.WerewolfGameManager
import dev.slne.surf.event.werewolf.util.GameState
import dev.slne.surf.event.werewolf.util.NightAction
import dev.slne.surf.event.werewolf.util.NightStep
import dev.slne.surf.event.werewolf.util.WerwolfRoles
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

fun amorWerewolfCommand() = subcommand("amor") {
    withArguments(EntitySelectorArgument.OnePlayer("firstPlayer"))
    withArguments(EntitySelectorArgument.OnePlayer("secondPlayer"))

    playerExecutor { commandSender, arguments ->
        val firstPlayer = arguments.get("firstPlayer") as Player
        val secondPlayer = arguments.get("secondPlayer") as Player
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

        if (service.getPlayerRole(commandSender.uuid()) != WerwolfRoles.AMOR) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur Amor darf Liebespaare bestimmen.")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.AMOR) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Amor ist gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        if (firstPlayer.uniqueId == secondPlayer.uniqueId) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du musst zwei verschiedene Spieler auswählen.")
            }
            return@playerExecutor
        }

        val submitted = service.engine.submitNightAction(
            NightAction.AmorLink(
                actor = commandSender.uuid(),
                first = firstPlayer.uuid(),
                second = secondPlayer.uuid()
            )
        )

        if (!submitted) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Das Liebespaar konnte nicht gespeichert werden. Amor darf nur in der ersten Nacht handeln.")
            }
            return@playerExecutor
        }

        commandSender.sendText {
            appendSuccessPrefix()
            success("Du hast")
            appendSpace()
            variableValue(firstPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success("und")
            appendSpace()
            variableValue(secondPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success("als Liebespaar bestimmt.")
        }
    }
}
