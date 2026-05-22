package dev.slne.surf.survival.events.base.command


import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.pagination.Pagination
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.commands.PermissionRegistry
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

fun queueListCommand() = commandTree("queue-list") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    integerArgument("page", optional = true) {
        playerExecutor { player, args ->
            val page: Int = (args.get("page") as? Int) ?: 1
            player.showParticipants(page)
        }
    }
}

private fun Player.showParticipants(page: Int) {
    val players = GameService.getQueuePlayers()
        .map { Bukkit.getOfflinePlayer(it) }

    if (players.isEmpty()) {
        sendText {
            appendInfoPrefix()
            info("Es ist niemand in der Warteschlange.")
        }
        return
    }
    val pagination = Pagination<OfflinePlayer> {
        title {
            primary("Spieler in der Warteschlange")
            spacer(" | (${players.size})")
        }
        rowRenderer { row, _ ->
            val player = row.player
            val displayName = row.name ?: "#Unbekannt"
            listOf(
                buildText {
                    append(
                        buildText {
                            variableValue(displayName)
                        }.clickEvent(ClickEvent.callback { audience ->
                            if (player != null) {
                                audience.sendText {
                                    appendInfoPrefix()
                                    info("Du hast")
                                    appendSpace()
                                    variableValue(displayName)
                                    appendSpace()
                                    info("aus der Warteschlange entfernt.")
                                }

                                player.sendText {
                                    appendInfoPrefix()
                                    info("Du wurdest aus der Warteschlange entfernt.")
                                }
                                GameService.leaveWaitingQueue(player)
                                GameService.leaveGameQueue(player)
                            }
                        })
                    ).hoverEvent(HoverEvent.showText(buildText {
                        error("Klicke um den Spieler Entfernen.")
                    }))
                }
            )
        }
    }

    sendText {
        append(pagination.renderComponent(players, page))
    }
}

