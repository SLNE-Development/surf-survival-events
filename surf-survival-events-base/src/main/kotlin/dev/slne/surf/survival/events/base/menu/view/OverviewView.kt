package dev.slne.surf.survival.events.base.menu.view

import dev.slne.surf.api.paper.inventory.framework.dsl.onItemClick
import dev.slne.surf.api.paper.inventory.framework.view.layoutTarget
import dev.slne.surf.api.paper.inventory.framework.view.paginatedSurfView
import dev.slne.surf.api.paper.inventory.framework.view.pagination.pagination
import dev.slne.surf.api.paper.inventory.framework.view.settings
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginationViewRows
import dev.slne.surf.survival.events.base.game.GameRegistry

internal val eventGamesOverviewView = paginatedSurfView("Events") {
    settings {
        paginationViewRows(PaginationViewRows.ONE)
        navigateBackOnOutsideClick(false)
    }
    layoutTarget('G')
    pagination {
        computedSource { GameRegistry.getRegisteredGameKeys() }
        itemFactory { key ->
            withItem(key.createSkull())
            onItemClick {
                closeForPlayer()
                player.performCommand("survivalevents start ${key.key}")
            }
        }
    }
}