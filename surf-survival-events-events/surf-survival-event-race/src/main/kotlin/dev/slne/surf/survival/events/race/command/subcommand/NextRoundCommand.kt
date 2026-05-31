package dev.slne.surf.survival.events.race.command.subcommand

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.plugin
import dev.slne.surf.survival.events.race.service.RaceRoundAdvanceType
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceStage
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionList
import org.bukkit.Material

fun CommandAPICommand.nextRoundCommand() = subcommand("next-round") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("advance-count", 1, optional = true)

    playerExecutor { player, args ->
        val configuredDefault = when (RaceService.getRaceStage()) {
            RaceStage.FINAL -> RaceConfig.getConfig().gameplay.finalWinnerCount
            else -> RaceConfig.getConfig().gameplay.qualifiersPerRound
        }
        val advanceCount = args.get("advance-count") as? Int ?: configuredDefault

        plugin.launch {
            RegionService.fillBlocks(Material.BARRIER)
        }

        val result = RaceService.nextRound(advanceCount)

        player.sendText {
            when (result.type) {
                RaceRoundAdvanceType.NO_ACTIVE_RACE -> {
                    appendErrorPrefix()
                    error("Es ist kein Race Event aktiv.")
                }

                RaceRoundAdvanceType.RACE_NOT_RUNNING -> {
                    appendErrorPrefix()
                    error("Du kannst die nächste Runde nur auswerten, wenn das Rennen läuft.")
                }

                RaceRoundAdvanceType.NO_ACTIVE_RACERS -> {
                    appendErrorPrefix()
                    error("Es sind keine aktiven Racer eingetragen.")
                }

                RaceRoundAdvanceType.NEXT_HEAT_PREPARED -> {
                    appendSuccessPrefix()
                    success("Nächster Heat wurde vorbereitet.")
                    appendNewline()
                    info("Weiter:")
                    appendSpace()
                    variableValue(result.advanced.size.toString())
                    appendSpace()
                    info("| Ausgeschieden:")
                    appendSpace()
                    variableValue(result.eliminated.size.toString())
                    appendSpace()
                    info("| Neue Racer:")
                    appendSpace()
                    variableValue(result.nextRacers.size.toString())
                }

                RaceRoundAdvanceType.NEXT_STAGE_PREPARED -> {
                    appendSuccessPrefix()
                    success("Nächste Qualifikationsstufe wurde vorbereitet.")
                    appendNewline()
                    info("Weiter:")
                    appendSpace()
                    variableValue(result.advanced.size.toString())
                    appendSpace()
                    info("| Neue Racer:")
                    appendSpace()
                    variableValue(result.nextRacers.size.toString())
                }

                RaceRoundAdvanceType.FINAL_PREPARED -> {
                    appendSuccessPrefix()
                    success("Das Finale wurde vorbereitet.")
                    appendNewline()
                    info("Finalisten:")
                    appendSpace()
                    variableValue(result.nextRacers.size.toString())
                }

                RaceRoundAdvanceType.FINISHED -> {
                    appendSuccessPrefix()
                    success("Das Race Event wurde ausgewertet.")
                    appendNewline()
                    info("Gewinner:")
                    appendSpace()
                    variableValue(result.advanced.size.toString())
                }
            }
        }
    }
}
