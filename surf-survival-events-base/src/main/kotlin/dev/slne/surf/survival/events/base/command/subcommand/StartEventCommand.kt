package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.nestedArguments
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.command.argument.GameArgument
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.Games
import dev.slne.surf.survival.events.base.menu.dialog.setMaxPlayerDialog
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry

fun CommandTree.startEventCommand() {
    literalArgument("start") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        nestedArguments(
            GameArgument("game")
        ) {

            playerExecutor { player, args ->
                val game: Games by args

                if (!Games.isGameEnabled(game.name)) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Das Spiel")
                        appendSpace()
                        variableValue(game.displayName)
                        appendSpace()
                        error("ist derzeit nicht verfügbar.")
                    }
                    return@playerExecutor
                }

                if (!GameService.isGameActive()) {
                    player.showDialog(setMaxPlayerDialog(game))
                    return@playerExecutor
                }
                if (GameService.getActiveGame() != game) {
                    player.sendText {
                        appendErrorPrefix()
                        variableValue(GameService.getActiveGame().displayName)
                        appendSpace()
                        error("läuft bereits.")
                    }
                    return@playerExecutor
                }

                player.sendText {
                    appendSuccessPrefix()
                    success("Das Event wird gestartet...")
                }

                GameService.beginGame()
                GameService.stopGame()
            }
        }
    }
}