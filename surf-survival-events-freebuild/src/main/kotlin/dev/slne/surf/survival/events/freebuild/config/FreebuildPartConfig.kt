package dev.slne.surf.survival.events.freebuild.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.survival.events.freebuild.plugin
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class FreebuildPartConfig(
    var enableSurvivalEventsNpc: Boolean = false,
    val eventServerName: String = "survival-events",
    val eventServerDisplayName: String = "",
    val certificateItemModel: String = ""
) {
    companion object : SpongeYmlConfigClass<FreebuildPartConfig>(
        FreebuildPartConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )
}
