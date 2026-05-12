package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceState
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun startCountDownCommand() = subcommand("CountDown") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    anyExecutor { sender, _ ->
        if (RaceService.getRaceState() == RaceState.DEACTIVATED) {
            sender.sendText {
                appendErrorPrefix()
                error("Race ist nicht Aktiv.")
            }
            return@anyExecutor
        }

        if (RaceService.getRaceState() == RaceState.WAITING) {

            sender.sendText {
                appendSuccessPrefix()
                success("Das Spiel Started...")
            }
            RaceService.setRaceState(RaceState.COUNTDOWN)
            RaceService.startCountdown()
            return@anyExecutor
        }

        sender.sendText {
            appendErrorPrefix()
            error("Du kannst den Command nicht ausführen.")
            appendNewline()
            error("Aktuelle State:")
            appendSpace()
            variableValue(RaceService.getRaceState().name)
        }
    }
}



