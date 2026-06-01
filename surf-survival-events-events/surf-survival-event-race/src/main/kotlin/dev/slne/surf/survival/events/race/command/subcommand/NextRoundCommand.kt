package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.game.RaceGame
import dev.slne.surf.survival.events.race.service.RaceRoundAdvanceType
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceStage
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionList
import org.bukkit.Material

fun CommandAPICommand.nextRoundCommand() = subcommand("next-round") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    integerArgument("advance-count", 1, optional = true)

    playerExecutorSuspend { player, args ->
        val configuredDefault = when (RaceService.getRaceStage()) {
            RaceStage.FINAL -> RaceConfig.getConfig().gameplay.finalWinnerCount
            else -> RaceConfig.getConfig().gameplay.qualifiersPerRound
        }
        val advanceCount = args.get("advance-count") as? Int ?: configuredDefault

        val result = GameService.withGameContext(RaceGame.KEY) {
            RegionService.fillBlocks(Material.BARRIER)
            RaceService.nextRound(advanceCount)
        }

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
