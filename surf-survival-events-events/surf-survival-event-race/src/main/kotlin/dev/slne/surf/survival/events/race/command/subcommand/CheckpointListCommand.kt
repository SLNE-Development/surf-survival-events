package dev.slne.surf.survival.events.race.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.base.game.GameWorldService
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.game.RaceGame
import dev.slne.surf.survival.events.race.utils.PermissionList
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox

fun CommandAPICommand.checkpointListCommand() = subcommand("list") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)
    multiLiteralArgument("arguments", "checkpoints", "start", "barrier")

    playerExecutor { player, args ->
        val arguments: String by args

        player.sendText {
            when (arguments) {
                "start" -> {
                    val start = RaceConfig.getConfig().starts
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
                    val barrier = RaceConfig.getConfig().barriers
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
                    val checkpoints = RaceConfig.getConfig().checkpoints
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

private fun sendCheckpoint(player: Player, checkpoint: RaceConfig.CheckPointConfig) {
    val world = GameWorldService.getEventWorld(RaceGame.KEY)

    player.sendText {
        success("#${checkpoint.id}")
        appendNewline()
        append(
            buildText {
                variableValue("${world ?: "Welt nicht geladen"} | (${checkpoint.x1} ${checkpoint.y1} ${checkpoint.z1})")
            }.clickEvent(ClickEvent.callback {
                val world = GameWorldService.getEventWorld(RaceGame.KEY) ?: return@callback
                player.teleportAsync(Location(world, checkpoint.x1, checkpoint.y1, checkpoint.z1))
                    .thenRun {
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
                player.teleportAsync(Location(world, checkpoint.x2, checkpoint.y2, checkpoint.z2))
                    .thenRun {
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

private fun sendStart(player: Player, start: RaceConfig.StartConfig) {
    val world = GameWorldService.getEventWorld(RaceGame.KEY)

    player.sendText {
        append(
            buildText {
                variableValue("${world ?: "Welt nicht geladen"} | (${start.x1} ${start.y1} ${start.z1})")
            }.clickEvent(ClickEvent.callback {
                val world = GameWorldService.getEventWorld(RaceGame.KEY) ?: return@callback
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

private fun sendBarrier(player: Player, barrier: BoundingBox) {
    val world = GameWorldService.getEventWorld(RaceGame.KEY)

    player.sendText {
        append(
            buildText {
                variableValue("${world ?: "Welt nicht geladen"} | (${barrier.minX} ${barrier.minY} ${barrier.minZ})")
            }.clickEvent(ClickEvent.callback {
                val world = GameWorldService.getEventWorld(RaceGame.KEY) ?: return@callback

                player.teleportAsync(Location(world, barrier.minX, barrier.minY, barrier.minZ)).thenRun {
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
                variableValue("(${barrier.maxX} ${barrier.maxY} ${barrier.maxZ})")
            }.clickEvent(ClickEvent.callback {
                val world = GameWorldService.getEventWorld(RaceGame.KEY) ?: return@callback

                player.teleportAsync(Location(world, barrier.maxX, barrier.maxY, barrier.maxZ)).thenRun {
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