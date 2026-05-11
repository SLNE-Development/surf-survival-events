package dev.slne.surf.survival.events.base.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.games.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit

fun queueListCommand() = subcommand("queuelist") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    anyExecutor { sender, _ ->

        sender.sendText {
            appendInfoPrefix()

        }

        sender.sendText {
            info("Spieler in der Warteschlange:")
            appendNewline()
            GameService.getQueuePlayers().forEach { uuid ->
                val player = Bukkit.getPlayer(uuid)
                append(
                    buildText {
                        variableValue(player?.name ?: "#Unbekannt")
                    }.clickEvent(ClickEvent.callback {
                        if (player != null) {
                            player.sendText {
                                appendInfoPrefix()
                                info("Du wurdest aus der Warteschlange entfernt.")
                            }
                            GameService.leaveWaitingQueue(player)
                            GameService.leaveGameQueue(player)
                        }
                    })
                )
                appendNewline()
            }

            hoverEvent(HoverEvent.showText(buildText {
                error("Klicke um den Spieler Entfernen.")
            }))
        }

    }
}

