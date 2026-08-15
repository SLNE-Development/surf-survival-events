package dev.slne.surf.survival.events.werewolf.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.werewolf.util.toBukkitPlayer
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.plugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

data class HiddenPlayerPair(
    val viewerId: UUID,
    val targetId: UUID,
)

object WerewolfVisibilityCleanup {

    private val pendingRestores = ConcurrentHashMap.newKeySet<HiddenPlayerPair>()

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
        val pairsToRestore = pendingRestores.filter(predicate)

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
        WerewolfCommandRequirements.update(event.player)
        WerewolfVisibilityCleanup.restorePendingForPlayers(listOf(event.player.uniqueId))
    }
}
