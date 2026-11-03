package dev.slne.surf.survival.events.freebuild.certificate

import java.time.LocalDate
import java.util.*

/** Tab completions and lookups for the `/certificate` subcommands, based on certificates sorted newest first. */
object CertificateSuggestions {
    private const val LIMIT = 20

    /** The dates offered for a new certificate: today, then the days of earlier events. */
    fun dates(certificates: List<CertificateRecord>, today: LocalDate): List<String> =
        (sequenceOf(today) + certificates.asSequence().map { it.date })
            .distinct()
            .take(LIMIT)
            .map(CertificateDates::format)
            .toList()

    /** Every event name once, regardless of case. */
    fun eventNames(certificates: List<CertificateRecord>): List<String> =
        certificates.asSequence()
            .distinctBy { it.eventName.lowercase(Locale.ROOT) }
            .take(LIMIT)
            .map { it.eventName }
            .toList()

    /** The event names of the certificates of the player last known as [playerName], ignoring case. */
    fun eventNamesOf(certificates: List<CertificateRecord>, playerName: String): List<String> =
        eventNames(certificates.filter { it.playerName.equals(playerName, ignoreCase = true) })

    /** The certificates for the event [eventName], compared like the event names of new certificates. */
    fun forEvent(certificates: List<CertificateRecord>, eventName: String): List<CertificateRecord> {
        val normalized = CertificateForm.normalizeEventName(eventName)
        return certificates.filter { it.eventName.equals(normalized, ignoreCase = true) }
    }
}
