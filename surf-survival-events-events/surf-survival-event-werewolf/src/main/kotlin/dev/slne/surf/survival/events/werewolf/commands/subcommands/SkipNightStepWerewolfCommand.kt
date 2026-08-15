package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements

fun skipNightStepWerewolfCommand() = subcommand("skipNightStep") {
    withRequirement { sender -> WerewolfCommandRequirements.canDebugSkipNightStep(sender) }
    playerExecutor { player, _ ->
        val service = WerewolfGameManager.getGameForPlayer(player.uniqueId)

        if (service == null) {
            player.sendText {
                appendErrorPrefix()
                error("Du leitest aktuell kein Werwolf-Spiel.")
            }
            return@playerExecutor
        }

        if (service.leader != player.uniqueId) {
            player.sendText {
                appendErrorPrefix()
                error("Nur der Erzähler darf den Nightstep überspringen.")
            }
            return@playerExecutor
        }

        if (service.isPhaseTransitioning) {
            player.sendText {
                appendErrorPrefix()
                error("Der Phasenwechsel läuft gerade noch.")
            }
            return@playerExecutor
        }

        if (!service.engine.skipCurrentNightStep()) {
            player.sendText {
                appendErrorPrefix()
                error("Es gibt gerade keinen überspringbaren Nightstep.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendSuccessPrefix()
            success("Der aktuelle Nightstep wurde übersprungen.")
        }
    }
}
