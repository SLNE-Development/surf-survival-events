package dev.slne.surf.survival.events.freebuild.papi.placeholder

import dev.slne.surf.api.paper.hook.papi.expansion.PapiPlaceholder
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import org.bukkit.OfflinePlayer

object IsEventActivePlaceholder : PapiPlaceholder("is-event-active") {
    override fun parse(
        player: OfflinePlayer,
        args: List<String>
    ) = FreebuildPartConfig.getConfig().enableSurvivalEventsNpc.toString()
}