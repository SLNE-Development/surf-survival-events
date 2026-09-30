package dev.slne.surf.survival.events.red.light.green.light.command.util

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.survival.events.red.light.green.light.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

object AreaSelectionActionBar {
    private val jobs = ConcurrentHashMap<UUID, Job>()

    fun start(player: Player, target: AreaTarget) {
        jobs.put(player.uniqueId, plugin.scope.runAtFixedRate(1.seconds) {
            player.sendActionBar(
                buildText {
                    success("${target.displayName}-Area-Set-Modus aktiv")
                }
            )
        })?.cancel()
    }

    fun stop(uuid: UUID) {
        jobs.remove(uuid)?.cancel()
    }
}