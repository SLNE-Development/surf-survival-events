package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.commands.argument.werewolfGameArgument
import dev.slne.surf.survival.events.werewolf.service.WerewolfJoinResult
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager

fun joinWerewolfCommand() = subcommand("join") {
    werewolfGameArgument("gameId")
    playerExecutor { player, arguments ->
        val game = arguments.get("gameId") as WerewolfService

        when (val result = game.join(player.uniqueId)) {
            WerewolfJoinResult.Success -> {
                WerewolfGameManager.joinGame(game.gameId, player.uniqueId)
                player.sendText {
                    appendSuccessPrefix()
                    success("Du bist dem Werwolf-Spiel '${game.gameId}' erfolgreich beigetreten!")
                }
            }

            WerewolfJoinResult.AlreadyInGame -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Du nimmst bereits an diesem Spiel teil.")
                }
            }

            WerewolfJoinResult.AlreadyStarted -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Das Spiel '${game.gameId}' läuft bereits, du kannst nicht mehr beitreten.")
                }
            }

            is WerewolfJoinResult.Error -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Fehler beim Beitreten: ${result.message}")
                }
            }
        }
    }
}
