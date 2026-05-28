package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun CommandTree.removeCheckpoint() {
    literalArgument("remove-Checkpoint") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        integerArgument("checkpointNumber") {

            playerExecutor { player, args ->
                val checkpointNumber: Int by args

                SurfRaceConfig.edit {

                    val removed = SurfRaceConfig.getConfig().checkPoints.removeIf { it.id == checkpointNumber }

                    if (!removed) {
                        player.sendText {
                            appendErrorPrefix()
                            error("Kein Checkpoint mit der Nummer")
                            appendSpace()
                            variableValue(checkpointNumber.toString())
                            appendSpace()
                            error("gefunden.")
                        }
                        return@playerExecutor
                    }

                    checkPoints.forEach {
                        if (it.id > checkpointNumber) {
                            it.id -= 1
                        }
                    }
                }
                SurfRaceConfig.save()

                player.sendText {
                    appendSuccessPrefix()
                    success("Du hast Checkpoint")
                    appendSpace()
                    variableValue(checkpointNumber.toString())
                    appendSpace()
                    success("gelöscht.")
                }
            }
        }
    }
}