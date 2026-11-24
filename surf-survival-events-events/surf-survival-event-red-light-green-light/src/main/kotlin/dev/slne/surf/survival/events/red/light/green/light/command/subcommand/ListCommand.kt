package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.pagination.Pagination
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.Bukkit
import java.util.UUID

fun CommandAPICommand.listCommand() = subcommand("list") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("page", optional = true)

    playerExecutor { player, arguments ->
        if (!GameService.isActiveGame(RedLightGreenLightGame.KEY)) {
            player.sendText {
                appendErrorPrefix()
                error("Red Light, Green Light ist nicht aktiv.")
            }
            return@playerExecutor
        }

        val page: Int? by arguments
        val members = RlglService.currentRoundMembers()

        if (members.isEmpty()) {
            player.sendText {
                appendInfoPrefix()
                info("Es ist aktuell niemand in einer Runde.")
            }
            return@playerExecutor
        }

        val pagination = Pagination<UUID> {
            title {
                primary("Runden-Teilnehmer")
                spacer(" (${members.size})")
            }

            rowRendererSimple { uuid ->
                buildText {
                    variableValue(Bukkit.getOfflinePlayer(uuid).name ?: uuid.toString())
                }
            }
        }

        player.sendText {
            append(pagination.renderComponent(members, page ?: 1))
        }
    }
}