package dev.slne.surf.survival.events.base.menu.dialog

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.*
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.games.service.AnnouncementService
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.games.util.Games
import dev.slne.surf.survival.events.base.menu.util.eventColored


@Suppress("UnstableApiUsage")
fun setMaxPlayerDialog(game: Games) = dialog {
    base {
        title { eventColored("Maximale Spieleranzahl") }
        body {
            plainMessage {
                info("Hier kannst du die maximale Anzahl an Spielern eingeben")
                appendNewline()
                appendNewline()
                appendWarningPrefix()
                error("Bitte beachte, dass die Zahl größer als 0 sein muss!")
            }

            input {
                text("max_players") {
                    label { eventColored("Maximale Spieleranzahl:") }
                    width(300)
                    maxLength(64)
                }
            }
        }

        type {
            confirmation(actionButton {
                label { error("Abbrechen") }
                tooltip { info("Klicke, um zurück zu gelangen.") }
                width(200)

                action {
                    customPlayerClick { _, player ->
                        player.closeDialog()
                    }
                }
            }, actionButton {
                label { success("Bestätigen") }
                tooltip { info("Klicke, um die Zahl zu bestätigen.") }
                width(200)

                action {
                    customPlayerClick { response, player ->
                        val maxPlayers = response.getText("max_players")?.trim()?.toIntOrNull()

                        if (maxPlayers == null || maxPlayers <= 0) {
                            player.closeDialog()

                            player.sendText {
                                appendErrorPrefix()
                                error("Bitte gib eine gültige Zahl ein.")
                            }
                            return@customPlayerClick
                        }

                        if (maxPlayers >= Int.MAX_VALUE) {
                            player.closeDialog()

                            player.sendText {
                                appendErrorPrefix()
                                error("Die Zahl ist zu groß.")
                            }
                            return@customPlayerClick
                        }

                        if (GameService.startGame(game, maxPlayers)) {
                            player.sendText {
                                appendSuccessPrefix()
                                variableValue(game.displayName)
                                appendSpace()
                                success("wurde erfolgreich aktiviert.")
                            }
                            for (onlinePlayer in server.onlinePlayers) {
                                AnnouncementService.sendOpenEvent(onlinePlayer)
                            }
                            player.closeDialog()
                            return@customPlayerClick
                        }

                         player.sendText {
                             appendErrorPrefix()
                             error("Ein Fehler ist aufgetreten.")
                         }
                    }
                }
            })
        }
    }

}