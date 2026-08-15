package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

fun killWerewolfCommand() = subcommand("kill") {
    withRequirement { sender -> WerewolfCommandRequirements.canActAsWerewolf(sender) }
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

        val currentPhase = service.engine.currentPhase
        if (currentPhase != GameState.NIGHT) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst diesen Befehl nur während der Nacht benutzen!")
            }
            return@playerExecutor
        }

        val playerRole = service.getPlayerRole(commandSender.uniqueId)
        if (playerRole != WerwolfRoles.WERWOLF) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst niemanden töten oder essen!")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.WEREWOLVES) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Die Werwölfe sind gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        if (targetPlayer == commandSender) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst dich nicht selber essen :)")
            }
            return@playerExecutor
        }

        val senderRole = service.getPlayerRole(commandSender.uniqueId)
        val targetPlayerRole = service.getPlayerRole(targetPlayer.uniqueId)

        if (senderRole == targetPlayerRole) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst keinen anderen Werwolf essen!")
            }
            return@playerExecutor
        }

        val submitted = service.engine.submitNightAction(
            NightAction.WerewolfKill(
                actor = commandSender.uniqueId,
                target = targetPlayer.uniqueId
            )
        )

        if (!submitted) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Deine Nachtaktion konnte nicht gespeichert werden.")
            }
            return@playerExecutor
        }

        commandSender.sendText {
            appendSuccessPrefix()
            success("Du hast den Dorfbewohner")
            appendSpace()
            variableValue(targetPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success("als dein Opfer ausgewählt!")
        }

        service.announceToRole(
            WerwolfRoles.WERWOLF,
            true,
            content = {
                appendInfoPrefix()
                info("")
            }
        )
    }
}
