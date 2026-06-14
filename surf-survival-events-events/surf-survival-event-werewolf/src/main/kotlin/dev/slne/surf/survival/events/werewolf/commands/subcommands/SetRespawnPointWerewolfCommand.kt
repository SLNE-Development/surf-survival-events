package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.werewolf.commands.argument.werewolfGameArgument
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.GamePhase
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements

fun setRespawnPointWerewolfCommand() = subcommand("setRespawnPoint") {
    withRequirement { sender -> WerewolfCommandRequirements.canStartGame(sender) }
    werewolfGameArgument("game")
    playerExecutor { player, arguments ->
        val game: WerewolfService by arguments

        if (game.leader != player.uniqueId || game.phase != GamePhase.LOBBY) {
            player.sendText {
                appendErrorPrefix()
                error("Du kannst den Respawnpoint nur fuer deine eigene offene Werwolf-Runde setzen.")
            }
            return@playerExecutor
        }

        val respawnPoint = player.location
        game.setEliminationRespawnPoint(respawnPoint)

        player.sendText {
            appendSuccessPrefix()
            success("Respawnpoint für ausgeschiedene Spieler gesetzt:")
            appendSpace()
            variableValue(respawnPoint.readableString(showRotation = true))
        }
    }
}
