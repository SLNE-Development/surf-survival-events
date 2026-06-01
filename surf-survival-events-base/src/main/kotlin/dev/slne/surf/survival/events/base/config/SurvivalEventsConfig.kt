package dev.slne.surf.survival.events.base.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.survival.events.base.plugin
import org.bukkit.Location
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
internal data class SurvivalEventsConfig(
    @param:Comment("Lobby of the event server. Players wait here before an event starts.")
    var serverLobby: Location = server.worlds.first().spawnLocation,

    @param:Comment("How online players are selected when /survivalevents start <game> is executed.")
    val start: EventStartConfig = EventStartConfig(),

    @param:Comment("Behaviour for players joining the event server while an event is idle/running.")
    val join: EventJoinConfig = EventJoinConfig()
) {
    companion object : SpongeYmlConfigClass<SurvivalEventsConfig>(
        SurvivalEventsConfig::class.java,
        plugin.dataPath,
        "config.yml"
    )

    @ConfigSerializable
    data class EventStartConfig(
        @param:Comment("Players with any of these permissions are ignored at event start, useful for staff.")
        var excludedPermissions: MutableSet<String> = mutableSetOf(),

        @param:Comment("Broadcasts a server message after the event was handed to the handler.")
        var announceStart: Boolean = true
    )

    @ConfigSerializable
    data class EventJoinConfig(
        @param:Comment("Teleport players to the server lobby if they join while no event is active.")
        var teleportToServerLobbyWhenIdle: Boolean = true,

        @param:Comment("Auto-register PlayerJoinEvent players while an event is running, based on GameOptions.")
        var autoJoinRunningEvent: Boolean = true,

        @param:Comment("Send a hint if a player joins while an event is running but cannot/does not auto-join.")
        var announceRunningEventOnJoin: Boolean = true
    )
}
