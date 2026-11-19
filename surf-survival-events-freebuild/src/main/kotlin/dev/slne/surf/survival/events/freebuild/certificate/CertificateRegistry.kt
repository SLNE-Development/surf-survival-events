package dev.slne.surf.survival.events.freebuild.certificate

import java.time.Instant
import java.time.LocalDate
import java.util.*

/** Outcome of [CertificateRegistry.beginClaim]. */
sealed interface ClaimStart {
    data class Started(val record: CertificateRecord) : ClaimStart
    data object InProgress : ClaimStart
    data object AlreadyClaimed : ClaimStart
    data object NotFound : ClaimStart
}

/** Outcome of [CertificateRegistry.reserve]. */
sealed interface Reservation {
    /** The certificate may be issued now; afterwards it is [added][CertificateRegistry.add] or released. */
    data object Reserved : Reservation

    /** The player already has a certificate for this event. */
    data class Exists(val record: CertificateRecord) : Reservation

    /** The certificate is being issued right now. */
    data object InProgress : Reservation
}

/**
 * In-memory index of all certificates and their claim state, by map id and by player.
 *
 * Every player gets at most one certificate per event and day. Issuing one starts with [reserve], which is atomic,
 * so two issues for the same player and event cannot both create a certificate.
 *
 * A claim is started with [beginClaim] and ended with either [finishClaim] or [abortClaim]; while it runs, further
 * claims of the same certificate are refused, so a certificate is never handed out twice.
 */
class CertificateRegistry {
    private val lock = Any()
    private val records = HashMap<Int, CertificateRecord>()
    private val recordsByPlayer = HashMap<UUID, MutableMap<Int, CertificateRecord>>()
    private val unreadable = HashSet<Int>()
    private val reserved = HashSet<EventKey>()
    private val claiming = HashSet<Int>()
    private val newestFirst = compareByDescending<CertificateRecord> { it.date }.thenByDescending { it.createdAt }

    /** Adds or replaces [record] and ends the reservation it was issued under. */
    fun add(record: CertificateRecord): Unit = synchronized(lock) {
        records[record.mapId] = record
        recordsByPlayer.getOrPut(record.playerUuid) { HashMap() }[record.mapId] = record
        unreadable.remove(record.mapId)
        reserved.remove(EventKey.of(record))
    }

    /**
     * Remembers [mapId] as a certificate whose metadata could not be read. It has no record, but its map keeps being
     * drawn and protected, and its id is never given to a new certificate.
     */
    fun addUnreadable(mapId: Int): Unit = synchronized(lock) {
        if (mapId !in records) {
            unreadable += mapId
        }
    }

    operator fun contains(mapId: Int): Boolean = synchronized(lock) {
        mapId in records || mapId in unreadable
    }

    operator fun get(mapId: Int): CertificateRecord? = synchronized(lock) {
        records[mapId]
    }

    /** The highest map id of all readable certificates; unreadable ones only come from file names. */
    fun highestMapId(): Int? = synchronized(lock) {
        records.keys.maxOrNull()
    }

    /** Makes the claimed certificate [mapId] claimable again and returns whether it was claimed. */
    fun reopen(mapId: Int): Boolean = synchronized(lock) {
        val record = records[mapId]
        if (record == null || !record.isClaimed) {
            return false
        }

        add(record.copy(claimedAt = null))
        true
    }

    /** All certificates of [playerUuid], newest event first. */
    fun certificatesOf(playerUuid: UUID): List<CertificateRecord> = synchronized(lock) {
        recordsOf(playerUuid).sortedWith(newestFirst)
    }

    /** All certificates, newest event first. */
    fun all(): List<CertificateRecord> = synchronized(lock) {
        records.values.sortedWith(newestFirst)
    }

    fun pendingFor(playerUuid: UUID): List<CertificateRecord> = synchronized(lock) {
        recordsOf(playerUuid)
            .filter { !it.isClaimed }
            .sortedBy { it.createdAt }
    }

    /** Whether [playerUuid] already got a certificate for the event [eventName] (ignoring case) on [date]. */
    fun hasCertificate(playerUuid: UUID, eventName: String, date: LocalDate): Boolean = synchronized(lock) {
        find(EventKey.of(playerUuid, eventName, date)) != null
    }

    fun reserve(playerUuid: UUID, eventName: String, date: LocalDate): Reservation = synchronized(lock) {
        val key = EventKey.of(playerUuid, eventName, date)
        val existing = find(key)

        when {
            existing != null -> Reservation.Exists(existing)
            !reserved.add(key) -> Reservation.InProgress
            else -> Reservation.Reserved
        }
    }

    /** Ends a reservation whose certificate could not be issued. */
    fun release(playerUuid: UUID, eventName: String, date: LocalDate): Unit = synchronized(lock) {
        reserved.remove(EventKey.of(playerUuid, eventName, date))
    }

    fun beginClaim(mapId: Int, playerUuid: UUID): ClaimStart = synchronized(lock) {
        val record = records[mapId]

        when {
            record == null || record.playerUuid != playerUuid -> ClaimStart.NotFound
            record.isClaimed -> ClaimStart.AlreadyClaimed
            !claiming.add(mapId) -> ClaimStart.InProgress
            else -> ClaimStart.Started(record)
        }
    }

    fun finishClaim(mapId: Int, claimedAt: Instant): CertificateRecord = synchronized(lock) {
        claiming.remove(mapId)
        val record = checkNotNull(records[mapId]) { "Unknown certificate map #$mapId" }.copy(claimedAt = claimedAt)
        add(record)
        record
    }

    fun abortClaim(mapId: Int): Unit = synchronized(lock) {
        claiming.remove(mapId)
    }

    private fun find(key: EventKey): CertificateRecord? =
        recordsOf(key.playerUuid).firstOrNull { EventKey.of(it) == key }

    private fun recordsOf(playerUuid: UUID): Collection<CertificateRecord> =
        recordsByPlayer[playerUuid]?.values ?: emptyList()

    private data class EventKey(val playerUuid: UUID, val eventName: String, val date: LocalDate) {
        companion object {
            fun of(playerUuid: UUID, eventName: String, date: LocalDate) =
                EventKey(playerUuid, eventName.lowercase(Locale.ROOT), date)

            fun of(record: CertificateRecord) = of(record.playerUuid, record.eventName, record.date)
        }
    }
}
