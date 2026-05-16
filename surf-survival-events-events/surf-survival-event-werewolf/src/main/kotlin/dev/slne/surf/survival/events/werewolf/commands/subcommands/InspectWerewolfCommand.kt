package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.uuid
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

fun inspectWerewolfCommand() = subcommand("inspect") {
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

        if (service.getPlayerRole(commandSender.uuid()) != WerwolfRoles.SEER) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur die Seherin darf Rollen aufdecken.")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.SEER) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Die Seherin ist gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        if (targetPlayer == commandSender) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Du kannst nicht deine eigene Rolle aufdecken.")
            }
            return@playerExecutor
        }

        val inspectedRole = service.engine.inspectWithSeer(
            actor = commandSender.uuid(),
            target = targetPlayer.uuid()
        )

        if (inspectedRole == null) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Deine Seher-Aktion konnte nicht gespeichert werden.")
            }
            return@playerExecutor
        }

        commandSender.sendText {
            appendSuccessPrefix()
            success("Die Rolle von")
            appendSpace()
            variableValue(targetPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success("ist")
            appendSpace()
            append(inspectedRole.displayName)
            spacer(".")
        }
    }
}
