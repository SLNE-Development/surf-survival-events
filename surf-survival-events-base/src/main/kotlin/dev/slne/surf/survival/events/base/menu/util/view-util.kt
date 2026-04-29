package dev.slne.surf.survival.events.base.menu.util


import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.survival.events.base.menu.util.eventColored
import me.devnatan.inventoryframework.context.Context
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound

fun Context.playGeneralClickSound() {
    player.playSound(true) {
        type(Sound.UI_BUTTON_CLICK)
    }
}

fun Context.playNewPageSound() {
    player.playSound(true) {
        type(Sound.ENTITY_CHICKEN_EGG)
    }
}

val previousItem = MenuHeads.ARROW_LEFT.clone().apply {
    displayName {
        eventColored("Vorherige Seite")
    }
}

val nextItem = MenuHeads.ARROW_RIGHT.clone().apply {
    displayName {
        eventColored("Nächste Seite")
    }
}

val backItem = MenuHeads.CROSS.clone().apply {
    displayName {
        primary("Zurück".toSmallCaps(), TextDecoration.BOLD)
    }
}

fun SurfComponentBuilder.eventColored(text: Any, vararg decoration: TextDecoration) =
    coloredComponent(text.toString(), TextColor.color(76, 161, 127), *decoration)