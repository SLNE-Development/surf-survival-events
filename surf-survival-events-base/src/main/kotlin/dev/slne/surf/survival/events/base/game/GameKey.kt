package dev.slne.surf.survival.events.base.game

import com.destroystokyo.paper.profile.ProfileProperty
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.survival.events.base.comp.eventColored
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.datacomponent.item.TooltipDisplay
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Stable identity of one event type.
 *
 * [key] is the command/config identity, for example `race`. It is also used by
 * [GameWorldService.worldKeyFor] to derive the default event world (`event_race`). Keep it stable
 * after release because configs, commands and world folders depend on it.
 *
 * [displayName] is only for UI/messages, and [skullTexture] is used by the event overview menu.
 */
public class GameKey<HANDLER : GameHandler> private constructor(
    /** Human-readable name shown in menus and command feedback. */
    public val displayName: String,

    /** Stable lowercase command key, for example `race` or `spleef`. */
    public val key: String,

    /** Base64 skull texture used by [createSkull]. */
    public val skullTexture: String,
) {

    public companion object {
        /** Creates a key directly. Prefer [builder] when call-site readability matters. */
        public fun <HANDLER : GameHandler> of(displayName: String, key: String, skullTexture: String): GameKey<HANDLER> {
            return GameKey(displayName, key, skullTexture)
        }

        /** Starts a typed builder for an event key. */
        public inline fun <reified HANDLER : GameHandler> builder(): Builder<HANDLER> = Builder()

        public class Builder<HANDLER : GameHandler> {
            private lateinit var displayName: String
            private lateinit var key: String
            private lateinit var skullTexture: String

            public fun displayName(displayName: String): Builder<HANDLER> = apply { this.displayName = displayName }
            public fun key(key: String): Builder<HANDLER> = apply { this.key = key }
            public fun skullTexture(skullTexture: String): Builder<HANDLER> = apply { this.skullTexture = skullTexture }

            public fun build(): GameKey<HANDLER> = GameKey(displayName, key, skullTexture)
        }
    }

    /** Creates the menu item used by the event overview. */
    @Suppress("UnstableApiUsage")
    public fun createSkull(): ItemStack = buildItem(Material.PLAYER_HEAD) {
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