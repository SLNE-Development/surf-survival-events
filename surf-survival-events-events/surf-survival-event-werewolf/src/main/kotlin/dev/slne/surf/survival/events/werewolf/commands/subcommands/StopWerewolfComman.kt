package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.commands.argument.werewolfGameArgument
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements

fun stopWerewolfCommand() = subcommand("stop") {
    withRequirement { sender -> WerewolfCommandRequirements.canStopGame(sender) }
    werewolfGameArgument("gameId")
    playerExecutor { player, arguments ->
        val game = arguments.get("gameId") as WerewolfService
        val participants = game.allParticipants

        game.stop()
        WerewolfGameManager.removeGame(game.gameId, participants)
        player.sendText {
            appendSuccessPrefix()
            success("Das Spiel '${game.gameId}' wurde erfolgreich beendet!")
        }
    }
}
