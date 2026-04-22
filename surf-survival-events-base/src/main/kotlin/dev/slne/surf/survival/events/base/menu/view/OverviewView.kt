package dev.slne.surf.survival.events.base.menu.view

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.paper.inventory.framework.outlineItem
import dev.slne.surf.api.paper.inventory.framework.titleBuilder
import dev.slne.surf.survival.events.base.games.util.Games
import dev.slne.surf.survival.events.base.menu.util.*
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.RenderContext
import net.kyori.adventure.text.format.TextDecoration

object OverviewView : View() {
    private val paginationState = buildLazyPaginationState { _ ->
        Games.entries.toMutableList()
    }.elementFactory { _, builder, _, game ->
        builder.withItem(game.createSkull()).onClick { context ->
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
}

