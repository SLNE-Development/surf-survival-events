package dev.slne.surf.event.werewolf.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.event.werewolf.util.toBukkitPlayer
import dev.slne.surf.survival.events.werewolf.plugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.Collections
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

data class HiddenPlayerPair(
    val viewerId: UUID,
    val targetId: UUID,
)

object WerewolfVisibilityCleanup {

    private val pendingRestores = Collections.synchronizedSet(mutableSetOf<HiddenPlayerPair>())

    fun queueRestore(hiddenPairs: Collection<HiddenPlayerPair>) {
        if (hiddenPairs.isEmpty()) return

        pendingRestores.addAll(hiddenPairs)
        restoreMatching(predicate = { true }, removeAfterRestore = false)

        plugin.launch {
            delay(1.seconds)
            restoreMatching(predicate = { true })
        }
    }

    fun restorePendingForPlayers(playerIds: Collection<UUID>) {
        if (playerIds.isEmpty()) return

        val relevantIds = playerIds.toSet()
        restoreMatching(predicate = { it.viewerId in relevantIds || it.targetId in relevantIds })
    }

    private fun restoreMatching(
        predicate: (HiddenPlayerPair) -> Boolean,
        removeAfterRestore: Boolean = true,
    ) {
        val pairsToRestore = synchronized(pendingRestores) {
            pendingRestores.filter(predicate)
        }

        if (pairsToRestore.isEmpty()) return

        plugin.launch {
            for (pair in pairsToRestore) {
                val viewer = pair.viewerId.toBukkitPlayer() ?: continue
                val target = pair.targetId.toBukkitPlayer() ?: continue

                withContext(plugin.entityDispatcher(viewer)) {
                    viewer.showPlayer(plugin, target)
                }

                if (removeAfterRestore) {
                    pendingRestores.remove(pair)
                }
            }
        }
    }
}

object WerewolfVisibilityCleanupListener : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        WerewolfVisibilityCleanup.restorePendingForPlayers(listOf(event.player.uniqueId))
    }
}
