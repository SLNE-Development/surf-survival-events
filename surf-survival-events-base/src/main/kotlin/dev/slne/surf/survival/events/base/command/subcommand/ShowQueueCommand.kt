package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.pagination.Pagination
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.PermissionRegistry
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

fun CommandTree.showQueueCommand() = literalArgument("queue") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)

    integerArgument("page", optional = true) {
        playerExecutor { player, args ->
            val page = args.get("page") as? Int ?: 1
            player.showParticipants(page)
        }
    }
}

private fun Player.showParticipants(page: Int) {
    val snapshot = GameService.snapshot()

    if (snapshot == null) {
        sendText {
            appendErrorPrefix()
            error("Derzeit ist kein Event aktiv.")
        }
        return
    }

    val players = snapshot.queuedPlayers.map(Bukkit::getOfflinePlayer)

    if (players.isEmpty()) {
        sendText {
            appendInfoPrefix()
            info("Es ist niemand in der Queue.")
        }
        return
    }

    val pagination = Pagination<OfflinePlayer> {
        title {
            primary("Spieler in der Queue")
            spacer(" | (${players.size})")
        }

        rowRenderer { row, _ ->
            val displayName = row.name ?: "#Unbekannt"

            listOf(
                buildText {
                    append(
                        buildText {
                            variableValue(displayName)
                        }.clickEvent(ClickEvent.callback { audience ->
                            val result = GameService.remove(row.uniqueId, includeSpectators = false)

                            if (result.removed) {
                                audience.sendText {
                                    appendInfoPrefix()
                                    info("Du hast")
                                    appendSpace()
                                    variableValue(displayName)
                                    appendSpace()
                                    info("aus der Queue entfernt.")
                                }

                                row.player?.sendText {
                                    appendInfoPrefix()
                                    info("Du bist aus der Queue geflogen.")
                                }
                            } else {
                                audience.sendText {
                                    appendErrorPrefix()
                                    error("$displayName ist nicht mehr in der Queue.")
                                }
                            }
                        })
                    ).hoverEvent(
                        HoverEvent.showText(
                            buildText {
                                error("Klicke, um den Spieler zu entfernen.")
                            }
                        )
                    )
                }
            )
        }
    }

    sendText {
        append(pagination.renderComponent(players, page))
    }
}