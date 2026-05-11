package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.RaceService
import dev.slne.surf.survival.events.race.service.RaceService.isGameActive
import dev.slne.surf.survival.events.race.utils.PermissionRegistry

fun startCountDownCommand() = subcommand("CountDown") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    anyExecutor { sender, _ ->
        if (!isGameActive()) {
            sender.sendText {
                appendErrorPrefix()
                error("Race ist nicht Aktiv.")
            }
            return@anyExecutor
        }

        if (!RaceService.isRaceStarted()) {
            sender.sendText {
                appendErrorPrefix()
                error("Der Timer ist bereits abgelaufen.")
            }
            return@anyExecutor
        }

        sender.sendText {
            appendSuccessPrefix()
            success("Das Spiel Started...")
        }
        RaceService.startCountdown()
    }
}



