package dev.slne.surf.event.werewolf.dialog

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.builder.actionButton
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.event.werewolf.inWerewolfColor
import dev.slne.surf.event.werewolf.service.WerewolfGameManager
import io.papermc.paper.registry.data.dialog.ActionButton
import org.bukkit.entity.Player

val DIALOG_TITLE = buildText { inWerewolfColor("Community Werwolf".toSmallCaps()) }

object WerewolfRoleViewDialoge {
    fun create(player: Player) = dialog {
        val uuid = player.uniqueId
        val game = WerewolfGameManager.getGameForPlayer(uuid)
        val isLeader = game?.leader == uuid

        base {
            title(DIALOG_TITLE)
            body {
                plainMessage(400) {

                    if (game == null) {
                        error("Du nimmst an keinem Werwolf-Spiel teil.")
                        return@plainMessage
                    }

                    if (!isLeader) {
                        val role = game.getPlayerRole(uuid) ?: run {
                            error("Deine Rolle konnte nicht gefunden werden.")
                            return@plainMessage
                        }

                        primary("🧩 Deine Rolle")
                        appendNewline()
                        spacer("─".repeat(40))
                        appendNewline()
                        appendNewline()

                        success("Rolle: ")
                        append(role.displayName)
                        appendNewline()
                        appendNewline()

                        info("Beschreibung:")
                        appendNewline()
                        spacer("  ")
                        append(role.description)
                        appendNewline()
                        appendNewline()

                        spacer("─".repeat(40))
                        appendNewline()
                        if (!role.isHostile) {
                            info("🌞 Das Dorf zählt auf dich.")
                        } else {
                            info("🌙 Die Nacht ist dein Verbündeter…")
                        }
                        appendNewline()
                        spacer("─".repeat(40))
                    }

                    else {
                        primary("📜 Rollenübersicht")
                        appendNewline()
                        spacer("─".repeat(40))
                        appendNewline()
                        appendNewline()

                        game.getAllPlayers().forEach { wwPlayer ->
                            val role = wwPlayer.role
                            val name = server.getPlayer(wwPlayer.uuid)?.name ?: "Unbekannt"

                            val deathState = if (wwPlayer.isAlive) "🟢 lebt" else "🔴 tot"
                            val loveState = wwPlayer.inLoveWith?.let {
                                val loverName = server.getPlayer(it)?.name ?: "Unbekannt"
                                "💘 verliebt in $loverName"
                            } ?: "🤍 nicht verliebt"

                            success("👤 $name")
                            appendSpace()
                            info("→")
                            appendSpace()
                            append(role.displayName)
                            appendNewline()

                            spacer("  ")
                            info(deathState)
                            appendSpace()
                            info("•")
                            appendSpace()
                            info(loveState)
                            appendNewline()

                            spacer("─".repeat(40))
                            appendNewline()
                        }
                    }
                }
            }
        }
        type {
            notice(exitButton())
        }
    }
}

private fun exitButton(): ActionButton = actionButton {
    label { spacer("Schließen") }
    tooltip { info("Klicke hier, um das Menü zu verlassen.") }
    action {
        playerCallback { it.closeDialog() }
    }
}