package dev.slne.surf.event.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
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

fun witchWerewolfCommand() = subcommand("witch") {
    multiLiteralArgument("action", "heal", "kill")
    withArguments(EntitySelectorArgument.OnePlayer("player"))

    playerExecutor { commandSender, arguments ->
        val action = (arguments.get("action") as String).lowercase()
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

        if (service.getPlayerRole(commandSender.uuid()) != WerwolfRoles.WITCH) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Nur die Hexe darf diesen Befehl benutzen.")
            }
            return@playerExecutor
        }

        if (service.engine.currentNightStep != NightStep.WITCH) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Die Hexe ist gerade nicht am Zug.")
            }
            return@playerExecutor
        }

        val nightAction = when (action) {
            "heal" -> NightAction.WitchHeal(
                actor = commandSender.uuid(),
                target = targetPlayer.uuid()
            )

            "kill" -> NightAction.WitchPoison(
                actor = commandSender.uuid(),
                target = targetPlayer.uuid()
            )

            else -> {
                commandSender.sendText {
                    appendErrorPrefix()
                    error("Nutze /werewolf witch <heal|kill> <spieler>.")
                }
                return@playerExecutor
            }
        }

        val submitted = service.engine.submitNightAction(nightAction)
        if (!submitted) {
            commandSender.sendText {
                appendErrorPrefix()
                error("Deine Hexen-Aktion konnte nicht gespeichert werden.")
            }
            return@playerExecutor
        }

        commandSender.sendText {
            appendSuccessPrefix()
            success(
                when (nightAction) {
                    is NightAction.WitchHeal -> "Du hast"
                    is NightAction.WitchPoison -> "Du hast"
                    else -> "Du hast"
                }
            )
            appendSpace()
            variableValue(targetPlayer.name, TextDecoration.BOLD)
            appendSpace()
            success(
                when (nightAction) {
                    is NightAction.WitchHeal -> "mit deinem Heiltrank ausgewählt."
                    is NightAction.WitchPoison -> "mit deinem Gifttrank ausgewählt."
                    else -> "."
                }
            )
        }
    }
}
