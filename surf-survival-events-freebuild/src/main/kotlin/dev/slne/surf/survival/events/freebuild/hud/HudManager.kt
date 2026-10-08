package dev.slne.surf.survival.events.freebuild.hud

import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.paper.util.forEachPlayer
import dev.slne.surf.hud.api.hud
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player

object HudManager {
    private val infoColor = TextColor.color(0xC5F1FC)
    private const val HUD_ID = "freebuild-event-join"

    fun show(player: Player) {
        player.hud.add {
            line(1) {
                element(HUD_ID, 0, buildText {
                    white("Betrete das Survival Event mit ")
                    append {
                        text("/survivalevents", infoColor)
                        decorate(TextDecoration.BOLD)
                    }
                })
            }
        }
    }

    fun handleUpdate() {
        if (FreebuildPartConfig.getConfig().enableSurvivalEventsNpc) {
            forEachPlayer {
                show(it)
            }
        } else {
            forEachPlayer {
                it.hud.remove(HUD_ID)
            }
        }
    }
}