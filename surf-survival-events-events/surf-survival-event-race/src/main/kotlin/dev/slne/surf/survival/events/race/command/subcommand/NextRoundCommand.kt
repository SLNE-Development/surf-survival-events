package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Material

fun CommandTree.nextRoundCommand() {
    literalArgument("next-round") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        integerArgument("rawInt", 1) {

            playerExecutor { player, args ->
                val rawInt: Int by args

                if (RaceService.getRaceState() != RaceState.RUNNING) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Du kannst die nächste Runde nur starten, wenn das Rennen läuft.")
                    }
                    return@playerExecutor
                }

                RegionService.fillBlocks(Material.BARRIER)
                RaceService.nextRound(rawInt)
                player.sendText {
                    appendSuccessPrefix()
                    success("Die Nächste Runde startet...")
                }
            }
        }
    }
}