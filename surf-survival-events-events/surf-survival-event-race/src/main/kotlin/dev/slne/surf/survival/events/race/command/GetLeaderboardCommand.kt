package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.service.ProgressService
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit

fun getLeaderboardCommand() = subcommand("get-leaderboard") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

    anyExecutor { sender, _ ->
        val places = ProgressService.getPlaceList()
        sender.sendText {
            text("--------------------------------------------------", TextColor.color(0x599542))
            appendNewline()
            info("Plätze:")
            appendNewline()
            places.forEach { uuid ->
                val player = Bukkit.getPlayer(uuid)
                variableValue(player?.name ?: uuid.toString())
                variableValue("#${places.indexOf(uuid) + 1}")
                if (places.size != places.indexOf(uuid)) {
                    appendNewline()
                }
            }
            text("--------------------------------------------------", TextColor.color(0x599542))
        }
    }
}