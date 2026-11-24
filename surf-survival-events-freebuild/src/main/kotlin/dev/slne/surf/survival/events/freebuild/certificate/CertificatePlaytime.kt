package dev.slne.surf.survival.events.freebuild.certificate

import dev.slne.surf.player.api.playtime.session.PlaytimeFilter
import dev.slne.surf.player.api.playtime.SurfPlaytimeApi
import dev.slne.surf.survival.events.freebuild.plugin
import org.bukkit.Bukkit
import java.time.LocalDate
import java.util.*
import java.util.logging.Level

/**
 * Reads event participation from surf-player. All access to surf-player classes stays in here, so the plugin
 * still loads when surf-player is missing.
 */
object CertificatePlaytime {
    private const val PLUGIN_NAME = "surf-player-paper"

    /**
     * Returns the playtime in seconds per player on [server] during [date], or `null` if surf-player is not
     * installed or older than 1.0.3.
     */
    suspend fun playtimeOn(server: String, date: LocalDate): Map<UUID, Long>? {
        if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return null
        }

        return try {
            SurfPlaytimeApi.getPlaytimePerPlayer(
                PlaytimeFilter(
                    from = date.atStartOfDay(),
                    to = date.plusDays(1).atStartOfDay(),
                    servers = setOf(server)
                )
            )
        } catch (error: LinkageError) {
            plugin.logger.log(Level.WARNING, "surf-player 1.0.3 or newer is required for certificates", error)
            null
        }
    }
}
