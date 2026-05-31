package dev.slne.surf.survival.events.base.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.survival.events.base.plugin
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class SurfRaceConfig(
    var eventManager: MutableList<EventManagerConfig> = mutableListOf()
    ) {
    companion object : SpongeYmlConfigClass<SurfRaceConfig>(
        SurfRaceConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class EventManagerConfig(
        var eventManagerWorld: String = "world",

        var eventManagerX: Double = -12.5,
        var eventManagerY: Double = 79.0,
        var eventManagerZ: Double = 15.5,
    )
}
