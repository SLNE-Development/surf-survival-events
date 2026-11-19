package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.playtime.api.common.session.PlaytimeFilter
import dev.slne.surf.playtime.api.common.surfPlaytimeApi
import dev.slne.surf.survival.events.freebuild.plugin
import org.bukkit.Bukkit
import java.time.LocalDate
import java.util.*
import java.util.logging.Level

/**
 * Reads event participation from surf-playtime. All access to surf-playtime classes stays in here, so the plugin
 * still loads when surf-playtime is missing.
 */
object CertificatePlaytime {
    private const val PLUGIN_NAME = "surf-playtime-paper"

    /**
     * Returns the playtime in seconds per player on [server] during [date], or `null` if surf-playtime is not
     * installed or older than 1.5.0.
     */
    suspend fun playtimeOn(server: String, date: LocalDate): Map<UUID, Long>? {
        if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return null
        }

        return try {
            surfPlaytimeApi.getPlaytimePerPlayer(
                PlaytimeFilter(
                    from = date.atStartOfDay(),
                    to = date.plusDays(1).atStartOfDay(),
                    servers = setOf(server)
                )
            )
        } catch (error: LinkageError) {
            plugin.logger.log(Level.WARNING, "surf-playtime 1.5.0 or newer is required for certificates", error)
            null
        }
    }
}
