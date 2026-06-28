package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

fun witchWerewolfCommand() = subcommand("witch") {
    withRequirement { sender -> WerewolfCommandRequirements.canActAsWitch(sender) }

    subcommand("heal") {
        withArguments(EntitySelectorArgument.OnePlayer("targetPlayer"))

        playerExecutor { commandSender, arguments ->
            val targetPlayer: Player by arguments
            commandSender.submitWitchAction(
                targetPlayer = targetPlayer,
                nightAction = NightAction.WitchHeal(
                    actor = commandSender.uniqueId,
                    target = targetPlayer.uniqueId
                )
            )
        }
    }

    subcommand("kill") {
        withArguments(EntitySelectorArgument.OnePlayer("targetPlayer"))

        playerExecutor { commandSender, arguments ->
            val targetPlayer: Player by arguments
            commandSender.submitWitchAction(
                targetPlayer = targetPlayer,
                nightAction = NightAction.WitchPoison(
                    actor = commandSender.uniqueId,
                    target = targetPlayer.uniqueId
                )
            )
        }
    }

    subcommand("skip") {
        playerExecutor { commandSender, _ ->
            val service = commandSender.getWitchServiceOrNotify() ?: return@playerExecutor

            if (!service.engine.skipCurrentNightStep(commandSender.uniqueId)) {
                commandSender.sendText {
                    appendErrorPrefix()
                    error("Der Hexen-Zug konnte nicht übersprungen werden.")
                }
                return@playerExecutor
            }

            commandSender.sendText {
                appendSuccessPrefix()
                success("Du hast deinen Hexen-Zug übersprungen.")
            }
        }
    }
}

private fun Player.submitWitchAction(
    targetPlayer: Player,
    nightAction: NightAction,
) {
    val service = getWitchServiceOrNotify() ?: return

    val submitted = service.engine.submitNightAction(nightAction)
    if (!submitted) {
        sendText {
            appendErrorPrefix()
            error("Deine Hexen-Aktion konnte nicht gespeichert werden.")
        }
        return
    }

    sendText {
        appendSuccessPrefix()
        success("Du hast")
        appendSpace()
        variableValue(targetPlayer.name, TextDecoration.BOLD)
        appendSpace()
        success(
            when (nightAction) {
                is NightAction.WitchHeal -> "mit deinem Heiltrank ausgewählt."
                is NightAction.WitchPoison -> "mit deinem Gifttrank ausgewählt."
                else -> "ausgewählt."
            }
        )
    }
}

private fun Player.getWitchServiceOrNotify(): WerewolfService? {
    val service = WerewolfGameManager.getGameForPlayer(uniqueId)

    if (service == null) {
        sendText {
            appendErrorPrefix()
            error("Du bist aktuell in keinem Werwolf-Spiel.")
        }
        return null
    }

    if (service.isPhaseTransitioning) {
        sendText {
            appendErrorPrefix()
            error("Der Phasenwechsel läuft gerade noch. Warte einen kurzen Moment.")
        }
        return null
    }

    if (service.engine.currentPhase != GameState.NIGHT) {
        sendText {
            appendErrorPrefix()
            error("Du kannst diesen Befehl nur während der Nacht benutzen!")
        }
        return null
    }

    if (service.getPlayerRole(uniqueId) != WerwolfRoles.WITCH) {
        sendText {
            appendErrorPrefix()
            error("Nur die Hexe darf diesen Befehl benutzen.")
        }
        return null
    }

    if (service.engine.currentNightStep != NightStep.WITCH) {
        sendText {
            appendErrorPrefix()
            error("Die Hexe ist gerade nicht am Zug.")
        }
        return null
    }

    return service
}
