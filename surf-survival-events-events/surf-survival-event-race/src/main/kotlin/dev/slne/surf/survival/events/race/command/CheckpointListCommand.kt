package dev.slne.surf.survival.events.race.command

import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
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

fun checkpointListCommand() = subcommand("list") {
    withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("arguments", "checkpoints", "start", "barrier")

    playerExecutor { player, args ->
        val arguments: String by args


        player.sendText {

            when (arguments) {
                "start" -> {
                    val start = SurfRaceConfig.getConfig().start
                    if (start.isEmpty()) {
                        player.sendText {
                            appendErrorPrefix()
                            error("Es gibt kein Start.")
                        }
                        return@playerExecutor
                    }

                    appendSuccessPrefix()
                    success("Startpunkt:")
                    start.forEach { start ->
                        sendStart(player, start)
                    }
                }

                "barrier" -> {
                    val barrier = SurfRaceConfig.getConfig().barrier
                    if (barrier.isEmpty()) {
                        player.sendText {
                            appendErrorPrefix()
                            error("Es gibt keine Barrier.")
                        }
                        return@playerExecutor
                    }

                    appendSuccessPrefix()
                    success("Barrier:")
                    barrier.forEach { barrier ->
                        sendBarrier(player, barrier)
                    }
                }

                "checkpoints" -> {
                    val checkpoints = SurfRaceConfig.getConfig().checkPoints
                    if (checkpoints.isEmpty()) {
                        player.sendText {
                            appendErrorPrefix()
                            error("Es gibt keine Checkpoints.")
                        }
                        return@playerExecutor
                    }

                    appendSuccessPrefix()
                    success("Checkpoints:")


                    checkpoints.sortedBy { it.id }.forEach { checkpoint ->
                        sendCheckpoint(player, checkpoint)
                    }
                }

                else -> {
                    appendErrorPrefix()
                    error("Das Argument existiert nicht.")
                }
            }
        }
    }
}

private fun sendCheckpoint(player: Player, checkpoint: SurfRaceConfig.Checkpoint) {

    val world = Bukkit.getWorld(checkpoint.world) ?: return

    player.sendText {
        success("#${checkpoint.id}")
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

private fun sendStart(player: Player, start: SurfRaceConfig.Start) {

    val world = Bukkit.getWorld(start.world) ?: return

    player.sendText {
        append(
            buildText {
                variableValue("${start.world} | (${start.x1} ${start.y1} ${start.z1})")
            }.clickEvent(ClickEvent.callback {
                player.teleportAsync(Location(world, start.x1, start.y1, start.z1)).thenRun {
                    player.sendText {
                        appendSuccessPrefix()
                        success("Teleportiert zu Startpunkt.")
                    }
                }
            })
        )

        appendSpace()
        info("->")
        appendSpace()

        append(
            buildText {
                variableValue("(${start.x2} ${start.y2} ${start.z2})")
            }.clickEvent(ClickEvent.callback {
                player.teleportAsync(Location(world, start.x2, start.y2, start.z2)).thenRun {
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

private fun sendBarrier(player: Player, barrier: SurfRaceConfig.Barrier) {

    val world = Bukkit.getWorld(barrier.world) ?: return

    player.sendText {
        append(
            buildText {
                variableValue("${barrier.world} | (${barrier.x1} ${barrier.y1} ${barrier.z1})")
            }.clickEvent(ClickEvent.callback {
                player.teleportAsync(Location(world, barrier.x1, barrier.y1, barrier.z1)).thenRun {
                    player.sendText {
                        appendSuccessPrefix()
                        success("Teleportiert zu Startpunkt.")
                    }
                }
            })
        )

        appendSpace()
        info("->")
        appendSpace()

        append(
            buildText {
                variableValue("(${barrier.x2} ${barrier.y2} ${barrier.z2})")
            }.clickEvent(ClickEvent.callback {
                player.teleportAsync(Location(world, barrier.x2, barrier.y2, barrier.z2)).thenRun {
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