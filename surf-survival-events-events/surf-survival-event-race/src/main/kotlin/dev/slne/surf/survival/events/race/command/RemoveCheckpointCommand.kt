package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun removeCheckpoint() = subcommand("remove-Checkpoint") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    integerArgument("checkpointNumber")

    playerExecutor { player, args ->
        val checkpointNumber: Int by args

        SurfRaceConfig.edit {

            val removed = SurfRaceConfig.getConfig().checkPoints.removeIf { it.int == checkpointNumber }

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
                if (it.int > checkpointNumber) {
                    it.int -= 1
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
