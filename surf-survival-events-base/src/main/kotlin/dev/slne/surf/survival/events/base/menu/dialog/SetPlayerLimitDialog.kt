package dev.slne.surf.survival.events.base.menu.dialog

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.*
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.service.AnnouncementService
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.Games
import dev.slne.surf.survival.events.base.menu.util.eventColored


@Suppress("UnstableApiUsage")
fun setMaxPlayerDialog(game: Games) = dialog {
    base {
        title { eventColored("Spielerlimit") }
        body {
            plainMessage {
                info("Hier kannst du die maximale Anzahl an Spielern eingeben")
                appendNewline()
                appendNewline()
                appendWarningPrefix()
                error("Bitte beachte, dass die Zahl größer als 0 sein muss!")
            }

            input {
                text("playerLimit") {
                    label { eventColored("Spielerlimit:") }
                    width(300)
                    maxLength(64)
                }
            }
        }

        type {
            confirmation(actionButton {
                label { success("Bestätigen") }
                tooltip { info("Klicke, um die Zahl zu bestätigen.") }
                width(200)

                action {
                    customPlayerClick { response, player ->
                        val playerLimit = response.getText("playerLimit")?.trim()?.toIntOrNull()

                        if (playerLimit == null || playerLimit <= 0) {
                            player.closeDialog()

                            player.sendText {
                                appendErrorPrefix()
                                error("Bitte gib eine gültige Zahl ein.")
                            }
                            return@customPlayerClick
                        }

                        if (playerLimit >= Int.MAX_VALUE) {
                            player.closeDialog()

                            player.sendText {
                                appendErrorPrefix()
                                error("Die Zahl ist zu groß.")
                            }
                            return@customPlayerClick
                        }

                        if (GameService.startGame(game, playerLimit)) {
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
            }, actionButton {
                label { error("Abbrechen") }
                tooltip { info("Zum Schließen klicken") }
                width(200)

                action {
                    customPlayerClick { _, player ->
                        player.closeDialog()
                    }
                }
            })
        }
    }

}