package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.werewolf.commands.argument.werewolfGameArgument
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements

fun stopWerewolfCommand() = subcommand("stop") {
    withRequirement { sender -> WerewolfCommandRequirements.canStopGame(sender) }
    werewolfGameArgument("game")
    
    playerExecutorSuspend { player, arguments ->
        val game: WerewolfService by arguments

        if (WerewolfGameManager.isBaseSession(game.gameId)) {
            GameService.stopGameAndWait(GameStopReason.COMMAND)
        } else {
            WerewolfGameManager.removeGame(game.gameId, game.allParticipants)
        }

        player.sendText {
            appendSuccessPrefix()
            success("Das Spiel '${game.gameId}' wurde erfolgreich beendet!")
        }
    }
}
