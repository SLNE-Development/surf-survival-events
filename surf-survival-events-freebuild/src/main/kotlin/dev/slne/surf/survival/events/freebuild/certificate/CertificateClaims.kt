package dev.slne.surf.survival.events.freebuild.certificate

import java.time.Instant
import java.util.*

/** Outcome of [CertificateClaims.claim]. */
sealed interface ClaimResult {
    data class Claimed(val record: CertificateRecord) : ClaimResult
    data object InventoryFull : ClaimResult
    data object AlreadyClaimed : ClaimResult
    data object InProgress : ClaimResult
    data object NotFound : ClaimResult
}

/**
 * Hands out pending certificates.
 *
 * The player's data is written to disk before the claim is persisted. A crash in between therefore offers the
 * certificate again after the restart instead of losing it; the second item shows the same map and is harmless.
 */
class CertificateClaims(
    private val registry: CertificateRegistry,
    private val persist: (CertificateRecord) -> Unit,
    private val clock: () -> Instant = Instant::now,
) {
    /**
     * Claims the pending certificate [mapId] of [playerUuid]. [handOver] puts the item into the inventory and returns
     * whether it fit, [savePlayer] writes the player's data to disk.
     */
    fun claim(
        playerUuid: UUID,
        mapId: Int,
        handOver: (CertificateRecord) -> Boolean,
        savePlayer: () -> Unit,
    ): ClaimResult {
        val record = when (val start = registry.beginClaim(mapId, playerUuid)) {
            is ClaimStart.Started -> start.record
            ClaimStart.InProgress -> return ClaimResult.InProgress
            ClaimStart.AlreadyClaimed -> return ClaimResult.AlreadyClaimed
            ClaimStart.NotFound -> return ClaimResult.NotFound
        }

        val handedOver = try {
            handOver(record)
        } catch (exception: Throwable) {
            registry.abortClaim(mapId)
            throw exception
        }

        if (!handedOver) {
            registry.abortClaim(mapId)
            return ClaimResult.InventoryFull
        }

        savePlayer()
        val claimed = registry.finishClaim(mapId, clock())
        persist(claimed)
        return ClaimResult.Claimed(claimed)
    }
}
