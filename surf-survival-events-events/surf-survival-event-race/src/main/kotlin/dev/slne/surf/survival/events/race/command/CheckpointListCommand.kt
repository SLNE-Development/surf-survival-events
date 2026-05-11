package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.utils.PermissionRegistry
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

fun checkpointListCommand() = subcommand("checkpoint-list") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)


    playerExecutor { player, _ ->
        val checkpoints = SurfRaceConfig.getConfig().checkPoints
        if (checkpoints.isEmpty()) {
            player.sendText {
                appendErrorPrefix()
                error("Es gibt keine Checkpoints.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendSuccessPrefix()
            success("Checkpoints:")

            checkpoints.sortedBy { it.int }.forEach { checkpoint ->
                sendCheckpoint(player, checkpoint)
            }
        }
    }
}

private fun sendCheckpoint(player: Player, checkpoint: SurfRaceConfig.Checkpoint) {

    val world = Bukkit.getWorld(checkpoint.world) ?: return

    player.sendText {
        success("#${checkpoint.int}")
        appendNewline()
        append(
            buildText {
                variableValue("${checkpoint.world} | (${checkpoint.x1} ${checkpoint.y1} ${checkpoint.z1})")
            }.clickEvent(ClickEvent.callback {
                player.teleportAsync(Location(world, checkpoint.x1, checkpoint.y1, checkpoint.z1)).thenRun {
                    player.sendText {
                        appendSuccessPrefix()
                        success("Teleportiert zu Startpunkt.")
                    }
                }
            })
        )

        hoverEvent(HoverEvent.showText(buildText {
            info("Klicke zum Teleportieren.")
        }))

        appendSpace()
        info("->")
        appendSpace()

        append(
            buildText {
                variableValue("(${checkpoint.x2} ${checkpoint.y2} ${checkpoint.z2})")
            }.clickEvent(ClickEvent.callback {
                player.teleportAsync(Location(world, checkpoint.x2, checkpoint.y2, checkpoint.z2)).thenRun {
                    player.sendText {
                        appendSuccessPrefix()
                        success("Teleportiert zu Endpunkt.")
                    }
                }
            })
        )

        hoverEvent(HoverEvent.showText(buildText {
            info("Klicke zum Teleportieren.")
        }))
    }
}