package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.commands.argument.werewolfGameArgument
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService

fun stopWerewolfCommand() = subcommand("stop") {
    werewolfGameArgument("gameId")
    playerExecutor { player, arguments ->
        val game = arguments.get("gameId") as WerewolfService

        game.stop()
        WerewolfGameManager.removeGame(game.gameId)
        player.sendText {
            appendSuccessPrefix()
            success("Das Spiel '${game.gameId}' wurde erfolgreich beendet!")
        }
    }
}
