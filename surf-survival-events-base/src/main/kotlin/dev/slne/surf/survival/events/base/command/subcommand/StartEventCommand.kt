package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.argument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.command.argument.GameKeyArgument
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.base.menu.dialog.createSetMaxPlayerDialog
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.PermissionRegistry

fun CommandTree.startEventCommand() = literalArgument("start") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    argument(GameKeyArgument("game")) {
        playerExecutorSuspend { player, args ->
            val game: GameKey<*> by args
            val activeGame = GameService.getActiveGameKeyOrNull()

            if (activeGame == null) {
                player.showDialog(createSetMaxPlayerDialog(game))
                return@playerExecutorSuspend
            }

            if (activeGame != game) {
                player.sendText {
                    appendErrorPrefix()
                    variableValue(activeGame.displayName)
                    appendSpace()
                    error("läuft bereits.")
                }
                return@playerExecutorSuspend
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