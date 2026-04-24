package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun changeMaxPlayersCommand() = subcommand("maxPlayers") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("string", "set", "info")
    integerArgument("maxPlayers", optional = true)
    playerExecutor { player, args ->
        val string: String by args
        val maxRawPlayers: Int? by args

        when (string) {
            "set" -> {
                if (!GameService.isGameActive()) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Es ist derzeit kein Event aktiv.")
                    }
                    return@playerExecutor
                }

                val maxPlayers = maxRawPlayers ?: run {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du musst eine Spieleranzahl angeben.")
                    }
                    return@playerExecutor
                }


                if (maxPlayers<= 0) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Die Spieleranzahl muss größer als 0 sein.")
                    }
                    return@playerExecutor
                }

                GameService.setMaxPlayers(maxPlayers)

                player.sendText {
                    appendSuccessPrefix()
                    success("Die maximale Spieleranzahl wurde auf")
                    appendSpace()
                    variableValue(maxRawPlayers.toString())
                    appendSpace()
                    success("gesetzt.")
                }



            }

           "info" -> {
               if (!GameService.isGameActive()) {
                   player.sendText {
                       appendErrorPrefix()
                       error("Es ist derzeit kein Event aktiv.")
                   }
               }

               player.sendText {
                   appendInfoPrefix()
                   info("Die maximale Spieleranzahl für das aktuelle Event beträgt:")
                   appendSpace()
                   variableValue(GameService.getMaxPlayers().toString())
               }
           }
        }
    }
}