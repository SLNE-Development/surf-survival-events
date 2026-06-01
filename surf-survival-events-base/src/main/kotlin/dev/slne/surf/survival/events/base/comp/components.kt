package dev.slne.surf.survival.events.base.comp


import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration


public fun SurfComponentBuilder.eventColored(text: Any, vararg decoration: TextDecoration): SurfComponentBuilder =
    coloredComponent(text.toString(), TextColor.color(76, 161, 127), *decoration)