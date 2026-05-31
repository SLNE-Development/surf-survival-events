package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.region.clearBoundingBoxCache
import dev.slne.surf.survival.events.race.utils.PermissionList

fun CommandAPICommand.removeCheckpoint() = subcommand("remove-checkpoint") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("checkpointNumber")

    playerExecutor { player, args ->
        val checkpointNumber: Int by args

        RaceConfig.edit {

            val removed = checkpoints.removeIf { it.id == checkpointNumber }

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

            checkpoints.forEach {
                if (it.id > checkpointNumber) {
                    it.id -= 1
                }
            }
        }
        clearBoundingBoxCache()
        RaceConfig.save()

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