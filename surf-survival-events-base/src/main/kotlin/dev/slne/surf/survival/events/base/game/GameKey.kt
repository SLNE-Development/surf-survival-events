package dev.slne.surf.survival.events.base.game

import com.destroystokyo.paper.profile.ProfileProperty
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.survival.events.base.menu.util.eventColored
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.datacomponent.item.TooltipDisplay
import org.bukkit.Material

class GameKey<HANDLER : GameHandler> private constructor(
    val displayName: String,
    val key: String,
    val skullTexture: String,
) {

    companion object {
        fun <HANDLER : GameHandler> of(displayName: String, key: String, skullTexture: String): GameKey<HANDLER> {
            return GameKey(displayName, key, skullTexture)
        }

        inline fun <reified HANDLER : GameHandler> builder() = Builder<HANDLER>()

        class Builder<HANDLER : GameHandler> {
            private lateinit var displayName: String
            private lateinit var key: String
            private lateinit var skullTexture: String

            fun displayName(displayName: String) = apply { this.displayName = displayName }
            fun key(key: String) = apply { this.key = key }
            fun skullTexture(skullTexture: String) = apply { this.skullTexture = skullTexture }

            fun build() = GameKey<HANDLER>(displayName, key, skullTexture)
        }
    }

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

    override fun toString(): String {
        return key
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameKey<*>) return false

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}