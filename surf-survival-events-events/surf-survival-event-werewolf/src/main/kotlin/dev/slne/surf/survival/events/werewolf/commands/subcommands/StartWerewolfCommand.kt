package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.commands.argument.werewolfGameArgument
import dev.slne.surf.survival.events.werewolf.service.WerewolfStartResult
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements

fun startWerewolfCommand() = subcommand("start") {
    withRequirement { sender -> WerewolfCommandRequirements.canStartGame(sender) }
    werewolfGameArgument("game")
    playerExecutor { player, arguments ->
        val game: WerewolfService by arguments

        when (val result = game.start()) {
            is WerewolfStartResult.Success -> {
                player.sendText {
                    appendSuccessPrefix()
                    success("Das Spiel '${game.gameId}' wurde erfolgreich gestartet!")
                }
            }

            is WerewolfStartResult.NotInLobbyPhase -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Das Spiel '${game.gameId}' läuft bereits.")
                }
            }

            is WerewolfStartResult.MissingRespawnPoint -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Start fehlgeschlagen! Setze zuerst den Respawnpoint fuer ausgeschiedene Spieler mit /werewolf setRespawnPoint ${game.gameId}.")
                }
            }

            is WerewolfStartResult.NotEnoughPlayers -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Start fehlgeschlagen! Es fehlen noch Spieler.")
                    appendSpace()
                    spacer("(")
                    variableValue(result.current)
                    spacer("/")
                    variableValue(result.required)
                    spacer(")")
                }
            }

            is WerewolfStartResult.Error -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist ein Fehler aufgetreten: ${result.message}")
                }
            }
        }
    }
}
