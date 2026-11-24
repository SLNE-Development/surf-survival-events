package dev.slne.surf.survival.events.freebuild.certificate

import java.util.*

/** A player that was on the event server for [seconds] on the event day. */
data class Participant(val uuid: UUID, val seconds: Long) {
    val minutes get() = seconds / 60
}

data class ParticipantSelection(
    val eligible: List<Participant>,
    val alreadyCertified: List<Participant>,
)

object Participants {
    /**
     * Picks every player from [playtime] (seconds by player) with at least [minimumMinutes] of playtime, longest
     * playtime first. Players for whom [hasCertificate] is true are listed separately instead.
     */
    fun select(
        playtime: Map<UUID, Long>,
        minimumMinutes: Int,
        hasCertificate: (UUID) -> Boolean,
    ): ParticipantSelection {
        val minimumSeconds = minimumMinutes * 60L
        val participants = playtime
            .filter { (_, seconds) -> seconds > 0 && seconds >= minimumSeconds }
            .map { (uuid, seconds) -> Participant(uuid, seconds) }
            .sortedWith(compareByDescending<Participant> { it.seconds }.thenBy { it.uuid })

        val (alreadyCertified, eligible) = participants.partition { hasCertificate(it.uuid) }
        return ParticipantSelection(eligible, alreadyCertified)
    }
}
