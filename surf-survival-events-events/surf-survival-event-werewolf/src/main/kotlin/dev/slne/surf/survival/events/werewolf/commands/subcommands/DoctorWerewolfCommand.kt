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

fun doctorWerewolfCommand() = subcommand("doctor") {
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

        if (service.engine.currentPhase != GameState.NIGHT) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst diesen Befehl nur während der Nacht benutzen!")
            }
            return@playerExecutor
        }

        if (service.getPlayerRole(commandSender.uuid()) != WerwolfRoles.DOCTOR) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur der Heiler darf diesen Befehl benutzen.")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.DOCTOR) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Der Heiler ist gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        val submitted = service.engine.submitNightAction(
            NightAction.DoctorProtect(
                actor = commandSender.uuid(),
                target = targetPlayer.uuid()
            )
        )

        if (!submitted) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst nur dich selbst oder das aktuelle Werwolf-Opfer heilen.")
            }
            return@playerExecutor
        }

        commandSender.sendText {
            appendSuccessPrefix()
            success("Du hast")
            appendSpace()
            variableValue(targetPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success("als Heilungsziel ausgewählt.")
        }
    }
}
