package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.argument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.command.argument.GameKeyArgument
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.service.GameService.StartGameType
import dev.slne.surf.survival.events.base.util.PermissionRegistry

internal fun CommandTree.startEventCommand() = literalArgument("start") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    argument(GameKeyArgument("game")) {
        playerExecutorSuspend { player, args ->
            val game: GameKey<*> by args

            player.sendText {
                appendInfoPrefix()
                info("Das Event")
                appendSpace()
                variableValue(game.displayName)
                appendSpace()
                info("wird gestartet...")
            }

            val result = GameService.startGame(game)

            when (result.type) {
                StartGameType.STARTED -> {
                    val context = result.context

                    player.sendText {
                        appendSuccessPrefix()
                        variableValue(game.displayName)
                        appendSpace()
                        success("wurde gestartet.")

                        if (context != null) {
                            appendNewline()
                            info("Spieler:")
                            appendSpace()
                            variableValue(context.activePlayerCount.toString())

                            if (context.reservePlayerCount > 0) {
                                appendSpace()
                                info("| Reserve:")
                                appendSpace()
                                variableValue(context.reservePlayerCount.toString())
                            }

                            if (context.spectatorCount > 0) {
                                appendSpace()
                                info("| Zuschauer:")
                                appendSpace()
                                variableValue(context.spectatorCount.toString())
                            }
                        }
                    }
                }

                StartGameType.ALREADY_ACTIVE -> {
                    val activeSession = result.activeSession
                    player.sendText {
                        appendErrorPrefix()
                        if (activeSession == null) {
                            error("Es läuft bereits ein Event.")
                        } else {
                            variableValue(activeSession.key.displayName)
                            appendSpace()
                            error("läuft bereits.")
                        }
                    }
                }

                StartGameType.NO_HANDLER_REGISTERED -> {
                    player.sendText {
                        appendErrorPrefix()
                        error("Für dieses Event ist kein GameHandler registriert.")
                    }
                }

                StartGameType.NOT_ENOUGH_PLAYERS -> {
                    player.sendText {
                        appendErrorPrefix()
                        error("Es sind nicht genug Spieler auf dem Event-Server.")
                        appendSpace()
                        info("Gefunden:")
                        appendSpace()
                        variableValue(result.selectedPlayers.toString())
                        appendSpace()
                        info("| Benötigt:")
                        appendSpace()
                        variableValue(result.minPlayers.toString())
                    }
                }

                StartGameType.HANDLER_FAILED -> {
                    player.sendText {
                        appendErrorPrefix()
                        error("Der GameHandler konnte das Event nicht starten. Details stehen in der Konsole.")
                    }
                }

                StartGameType.NO_ACTIVE_GAME -> {
                    player.sendText {
                        appendErrorPrefix()
                        error("Das Event konnte nicht gestartet werden, weil keine aktive Session erzeugt wurde.")
                    }
                }
            }
        }
    }
}