package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.service.JoinRoundResult
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.entity.Player

fun CommandAPICommand.addPlayerCommand() = subcommand("addPlayer") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    entitySelectorArgumentOnePlayer("target")
    stringArgument("id", optional = true)

    playerExecutor { player, arguments ->
        if (!GameService.isActiveGame(RedLightGreenLightGame.KEY)) {
            player.sendText {
                appendErrorPrefix()
                error("Red Light, Green Light ist nicht aktiv.")
            }
            return@playerExecutor
        }

        val target: Player by arguments
        val id: String? by arguments

        when (RlglService.joinRound(target, id)) {
            JoinRoundResult.JOINED -> {
                player.sendText {
                    appendSuccessPrefix()
                    variableValue(target.name)
                    appendSpace()
                    success("wurde zur Runde hinzugefügt.")
                }

                target.sendText {
                    appendSuccessPrefix()
                    success("Du wurdest von")
                    appendSpace()
                    variableValue(player.name)
                    appendSpace()
                    success("zur Runde hinzugefügt.")
                }
            }

            JoinRoundResult.ALREADY_JOINED -> {
                player.sendText {
                    appendErrorPrefix()
                    variableValue(target.name)
                    appendSpace()
                    error("ist der Runde bereits beigetreten.")
                }
            }

            JoinRoundResult.NO_ROUND_OPEN -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist aktuell keine Runde offen.")
                }
            }

            JoinRoundResult.WRONG_ID -> {
                player.sendText {
                    appendErrorPrefix()
                    error("Es ist keine Runde mit dieser ID offen.")
                }
            }
        }
    }
}