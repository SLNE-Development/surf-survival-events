package dev.slne.surf.event.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.event.werewolf.commands.werewolfGameArgument
import dev.slne.surf.event.werewolf.service.WerewolfStartResult
import dev.slne.surf.event.werewolf.service.WerewolfService

fun startWerewolfCommand() = subcommand("start") {
    werewolfGameArgument("gameId")
    playerExecutor { player, arguments ->
        val game = arguments.get("gameId") as WerewolfService

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