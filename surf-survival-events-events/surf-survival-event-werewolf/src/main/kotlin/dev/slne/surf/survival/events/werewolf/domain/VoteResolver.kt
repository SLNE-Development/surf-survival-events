package dev.slne.surf.survival.events.werewolf.domain

import dev.slne.surf.survival.events.werewolf.util.VoteStanding
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import java.util.*

internal object VoteResolver {

    fun calculateStandings(
        players: Map<UUID, WerewolfPlayer>,
        votes: Map<UUID, UUID>,
        weightSelector: (WerewolfPlayer) -> Int,
    ): List<VoteStanding> {
        if (votes.isEmpty()) return emptyList()

        val counts = mutableMapOf<UUID, Int>()

        for ((voter, target) in votes) {
            val voterPlayer = players[voter] ?: continue
            if (!voterPlayer.isAlive) continue
            if (players[target]?.isAlive != true) continue

            counts[target] = (counts[target] ?: 0) + weightSelector(voterPlayer)
        }

        return counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<UUID, Int>> { it.value }
                    .thenBy { players[it.key]?.name ?: "~" }
            )
            .map { VoteStanding(it.key, it.value) }
    }
}
