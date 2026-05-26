package dev.slne.surf.survival.events.race.command


import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.service.RegionService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import org.bukkit.Material

fun startRaceCommand() = subcommand("start") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    playerExecutor { player, _ ->
        if (RaceService.getRaceState() == RaceState.DEACTIVATED) {
            player.sendText {
                appendErrorPrefix()
                error("Race ist nicht Aktiv.")
            }
            return@playerExecutor
        }

        if (RaceService.getRaceState() == RaceState.LOBBY) {

            player.sendText {
                appendSuccessPrefix()
                success("Du hast dir die Spieler geholt")
            }

            RegionService.fillBlocks(Material.BARRIER)
            RaceService.playerToMid()

            return@playerExecutor
        }

        if (RaceService.getRaceState() == RaceState.WAITING) {

            player.sendText {
                appendSuccessPrefix()
                success("Das Spiel startet...")
            }
            RaceService.setRaceState(RaceState.COUNTDOWN)
            RaceService.startCountdown()
            return@playerExecutor
        }

        player.sendText {
            appendErrorPrefix()
            error("Du kannst den Command nicht ausführen.")
            appendNewline()
            error("Aktuelle State:")
            appendSpace()
            variableValue(RaceService.getRaceState().name)
        }
    }
}
