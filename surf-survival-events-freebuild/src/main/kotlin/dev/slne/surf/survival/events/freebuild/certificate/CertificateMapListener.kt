package dev.slne.surf.survival.events.freebuild.certificate

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.MapInitializeEvent

object CertificateMapListener : Listener {
    @EventHandler
    fun onMapInitialize(event: MapInitializeEvent) {
        CertificateService.onMapInitialize(event.map)
    }
}
