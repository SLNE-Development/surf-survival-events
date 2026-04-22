package dev.slne.surf.survival.events.paper.menu.view

import com.destroystokyo.paper.profile.ProfileProperty
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.survival.events.paper.games.util.Games
import dev.slne.surf.survival.events.paper.menu.util.backItem
import dev.slne.surf.survival.events.paper.menu.util.eventColored
import dev.slne.surf.survival.events.paper.menu.util.nextItem
import dev.slne.surf.survival.events.paper.menu.util.outlineItem
import dev.slne.surf.survival.events.paper.menu.util.playGeneralClickSound
import dev.slne.surf.survival.events.paper.menu.util.playNewPageSound
import dev.slne.surf.survival.events.paper.menu.util.previousItem
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.datacomponent.item.TooltipDisplay
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material

@Suppress("UnstableApiUsage")
object OverviewView : View() {

    private val paginationState = buildLazyPaginationState<Games> { _ ->
        Games.entries.toMutableList()
    }.elementFactory { _, builder, _, game ->
        builder.withItem(createSkullItem(game)).onClick { context ->
            context.playGeneralClickSound()
        }
    }.layoutTarget('G').build()


    override fun onInit(config: ViewConfigBuilder) {
        config
            .titleBuilder {
                eventColored("Events".toSmallCaps(), TextDecoration.BOLD)
            }
            .size(5)
            .layout(
                "OOOOOOOOO",
                "O-------O",
                "O-G-G-G-O",
                "O-------O",
                "OOOBXNOOO"
            )
            .cancelInteractions()
    }

    override fun onFirstRender(render: RenderContext) {
        val pagination = paginationState.get(render)

        render.layoutSlot('O', outlineItem)

        render.layoutSlot('X', backItem).onClick { context ->
            context.playGeneralClickSound()
            context.closeForPlayer()
        }

        render
            .layoutSlot('N')
            .renderWith {
                nextItem
            }
            .watch(paginationState)
            .displayIf { _ -> pagination.canAdvance() }
            .onClick { context ->
                context.playNewPageSound()
                pagination.advance()
            }

        render
            .layoutSlot('B')
            .renderWith {
                previousItem
            }
            .watch(paginationState)
            .displayIf { _ -> pagination.canBack() }
            .onClick { context ->
                context.playNewPageSound()
                pagination.back()
            }

    }

    private fun createSkullItem(game: Games) = buildItem(Material.PLAYER_HEAD) {

        displayName {
            eventColored(game.displayName.toSmallCaps())
        }

        setData(
            DataComponentTypes.PROFILE,
            ResolvableProfile.resolvableProfile()
                .addProperty(ProfileProperty("textures", game.skullTexture))
                .build()
        )

        setData(
            DataComponentTypes.TOOLTIP_DISPLAY,
            TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.PROFILE)
                .build()
        )

    }
}

