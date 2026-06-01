package dev.slne.surf.survival.events.base.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.pagination.Pagination
import dev.slne.surf.survival.events.base.game.ParticipantRole
import dev.slne.surf.survival.events.base.game.PlayerRemoveReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.PermissionRegistry
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.*

internal fun CommandTree.participantsCommand() = literalArgument("participants") {
    withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)

    integerArgument("page", optional = true) {
        playerExecutor { player, args ->
            val page = args.get("page") as? Int ?: 1
            player.showParticipants(page)
        }
    }
}

private data class ParticipantRow(
    val uuid: UUID,
    val role: ParticipantRole,
    val offlinePlayer: OfflinePlayer
)

private fun Player.showParticipants(page: Int) {
    val snapshot = GameService.snapshot()

    if (snapshot == null) {
        sendText {
            appendErrorPrefix()
            error("Derzeit ist kein Event aktiv.")
        }
        return
    }

    val rows = buildList {
        snapshot.gamePlayers.forEach { add(ParticipantRow(it, ParticipantRole.PLAYER, Bukkit.getOfflinePlayer(it))) }
        snapshot.reservePlayers.forEach {
            add(
                ParticipantRow(
                    it,
                    ParticipantRole.RESERVE,
                    Bukkit.getOfflinePlayer(it)
                )
            )
        }
        snapshot.spectators.forEach { add(ParticipantRow(it, ParticipantRole.SPECTATOR, Bukkit.getOfflinePlayer(it))) }
    }

    if (rows.isEmpty()) {
        sendText {
            appendInfoPrefix()
            info("Es ist niemand im Event eingetragen.")
        }
        return
    }

    val pagination = Pagination<ParticipantRow> {
        title {
            primary("Event-Teilnehmer")
            spacer(" | ${snapshot.status.name} | (${rows.size})")
        }

        rowRenderer { row, _ ->
            val displayName = row.offlinePlayer.name ?: "#Unbekannt"

            listOf(
                buildText {
                    append(
                        buildText {
                            variableValue(displayName)
                            appendSpace()
                            info("[")
                            variableValue(row.role.name)
                            info("]")
                        }.clickEvent(ClickEvent.callback { audience ->
                            val result = GameService.remove(
                                uuid = row.uuid,
                                includeSpectators = true,
                                reason = PlayerRemoveReason.KICK
                            )

                            if (result.removed) {
                                audience.sendText {
                                    appendInfoPrefix()
                                    info("Du hast")
                                    appendSpace()
                                    variableValue(displayName)
                                    appendSpace()
                                    info("aus dem Event entfernt.")
                                }

                                row.offlinePlayer.player?.sendText {
                                    appendInfoPrefix()
                                    info("Du wurdest aus dem Event entfernt.")
                                }
                            } else {
                                audience.sendText {
                                    appendErrorPrefix()
                                    error("$displayName ist nicht mehr im Event eingetragen.")
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
        append(pagination.renderComponent(rows, page))
    }
}
