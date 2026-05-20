package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.permissions.PermissionRegistry
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

private fun createClickable(gameId: String) = buildText {
    text("HIER", Colors.VARIABLE_VALUE, TextDecoration.UNDERLINED)
    hoverEvent(HoverEvent.showText(buildText { info("Klicke hier, um der Einladung zu folgen und dem Spiel '$gameId' beizutreten!") }))
    clickEvent(ClickEvent.runCommand("/werewolf join $gameId"))
}


fun openGameWerewolfCommand() = subcommand("openGame") {
    withRequirement { sender -> sender is Player && sender.hasPermission(PermissionRegistry.COMMAND_WEREWOLF_ADMIN) }
    stringArgument("gameId")
    playerExecutor { player, arguments ->
        val gameId = arguments.get("gameId") as String

        val game = WerewolfGameManager.createGame(gameId, player.uniqueId)
        if (game == null) {
            player.sendText {
                appendErrorPrefix()
                error("Spiel mit ID '$gameId' existiert bereits.")
            }
            return@playerExecutor
        }

        val potentialParticipants = player.getNearbyEntities(20.0, 20.0, 20.0)
            .filterIsInstance<Player>().filter { it != player }

        if (potentialParticipants.isEmpty()) {
            player.sendText {
                appendErrorPrefix()
                error("Es wurden keine Spieler in der Nähe gefunden, die dem Spiel beitreten könnten.")
            }
            return@playerExecutor
        } else {
            player.sendText {
                appendSuccessPrefix()
                success("Es wurden ${potentialParticipants.size} potenzielle Teilnehmer in der Nähe gefunden, die eingeladen werden mitzuspielen.")
            }
        }

        potentialParticipants.forEach { participant ->
            participant.sendText {
                appendInfoPrefix()
                info("Du wurdest von")
                appendSpace()
                variableValue(player.name)
                appendSpace()
                info("eingeladen, dem Werwolf-Spiel '$gameId' beizutreten!")

                appendNewInfoPrefixedLine()
                info("Klicke")
                appendSpace()
                append(createClickable(gameId))
                appendSpace()
                info("um der Einladung zu folgen und dem Spiel beizutreten.")

            }
        }
    }
}
