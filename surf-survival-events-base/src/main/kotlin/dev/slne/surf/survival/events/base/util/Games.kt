package dev.slne.surf.survival.events.base.util

import com.destroystokyo.paper.profile.ProfileProperty
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.extensions.pluginManager
import dev.slne.surf.survival.events.base.menu.util.eventColored
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.datacomponent.item.TooltipDisplay
import org.bukkit.Material

enum class Games(
    val displayName: String,
    private val skullTexture: String
) {
    EXAMPLE(
        "EXAMPLE",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ=="
    ),
    RACE(
        "RACE",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTEyNDNmN2U3MzhhMTI4MTc2YzUxNzUwMjY1MjRmMGU3NjhkZGU1MzBkZWRjMDU0Nzk3NDJjY2JiZTg5N2U1OCJ9fX0="
    );


    @Suppress("UnstableApiUsage")
    fun createSkull() = buildItem(Material.PLAYER_HEAD) {
        displayName {
            eventColored(displayName.toSmallCaps())
        }

        setData(
            DataComponentTypes.PROFILE,
            ResolvableProfile.resolvableProfile()
                .addProperty(ProfileProperty("textures", skullTexture))
                .build()
        )

        setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.PROFILE)
                .build()
        )

    }

    companion object {

        fun isGameEnabled(name: String) = pluginManager.isPluginEnabled("surf-survival-event-${name.lowercase()}")

        fun getGame(name: String) = entries.find { it.name.equals(name, ignoreCase = true) }
    }
}