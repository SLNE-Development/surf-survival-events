package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.entitySelectorArgumentOnePlayer
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.entity.Player

fun CommandTree.kickCommand() {
    literalArgument("kick") {
        withPermission(PermissionRegistry.COMMAND_GAME_SPECTATOR)
        entitySelectorArgumentOnePlayer("player") {

            anyExecutor { sender, args ->
                val player: Player by args

                if (RaceService.getRaceState() == RaceState.DEACTIVATED) {
                    sender.sendText {
                        appendErrorPrefix()
                        error("Das Event ist nicht Aktiv!")
                    }
                    return@anyExecutor
                }

                if (!RaceService.isInRace(player)) {
                    sender.sendText {
                        appendErrorPrefix()
                        error("Der Spieler")
                        appendSpace()
                        variableValue(player.name)
                        appendSpace()
                        error("ist nicht im Rennen.")
                    }
                    return@anyExecutor
                }

                if (RaceService.removePlayer(player)) {
                    sender.sendText {
                        appendSuccessPrefix()
                        success("Der Spieler")
                        appendSpace()
                        variableValue(player.name)
                        appendSpace()
                        success("wurde aus dem Rennen entfernt.")
                    }
                    return@anyExecutor
                }

                sender.sendText {
                    appendErrorPrefix()
                    error("Der Spieler")
                    appendSpace()
                    variableValue(player.name)
                    appendSpace()
                    error("wurde nicht gefunden.")
                }
            }
        }
    }
}