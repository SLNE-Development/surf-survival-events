package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuid
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.NightAction
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

fun serialKillerWerewolfCommand() = subcommand("serialkill") {
    withRequirement { sender -> WerewolfCommandRequirements.canActAsSerialKiller(sender) }
    withArguments(EntitySelectorArgument.OnePlayer("targetPlayer"))

    playerExecutor { commandSender, arguments ->
        val targetPlayer: Player by arguments
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

        if (service.getPlayerRole(commandSender.uuid()) != WerwolfRoles.SERIAL_KILLER) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur der Serienmörder darf diesen Befehl benutzen.")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.SERIAL_KILLER) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Der Serienmörder ist gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        val submitted = service.engine.submitNightAction(
            NightAction.SerialKillerKill(
                actor = commandSender.uuid(),
                target = targetPlayer.uuid()
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
            success("Du hast")
            appendSpace()
            variableValue(targetPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success("als dein Opfer ausgewählt.")
        }
    }
}
