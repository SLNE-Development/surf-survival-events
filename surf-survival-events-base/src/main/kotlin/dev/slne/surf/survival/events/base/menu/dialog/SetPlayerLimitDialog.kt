package dev.slne.surf.survival.events.base.menu.dialog

import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.survival.events.base.game.GameKey
import dev.slne.surf.survival.events.base.menu.util.eventColored
import dev.slne.surf.survival.events.base.service.AnnouncementService
import dev.slne.surf.survival.events.base.service.GameService


@Suppress("UnstableApiUsage")
fun createSetMaxPlayerDialog(gameKey: GameKey<*>) = dialog {
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
                    customPlayerClick(null, fun(response, player) {
                        val playerLimit = response.getText("playerLimit")?.trim()?.toIntOrNull()

                        if (playerLimit == null || playerLimit <= 0) {
                            player.closeDialog()

                            player.sendText {
                                appendErrorPrefix()
                                error("Bitte gib eine gültige Zahl ein.")
                            }
                            return
                        }

                        if (playerLimit >= Int.MAX_VALUE) {
                            player.closeDialog()

                            player.sendText {
                                appendErrorPrefix()
                                error("Die Zahl ist zu groß.")
                            }
                            return
                        }

                        if (GameService.startGame(gameKey, playerLimit)) {
                            player.sendText {
                                appendSuccessPrefix()
                                variableValue(gameKey.displayName)
                                appendSpace()
                                success("wurde erfolgreich aktiviert.")
                            }
                            player.closeDialog()
                            AnnouncementService.broadcastOpenEvent()

                            return
                        }

                        player.sendText {
                            appendErrorPrefix()
                            error("Ein Fehler ist aufgetreten.")
                        }
                    })
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