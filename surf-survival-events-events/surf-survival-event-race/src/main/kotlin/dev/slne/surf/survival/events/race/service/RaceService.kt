package dev.slne.surf.survival.events.race.service

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.title
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.game.ParticipantRole
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.plugin
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Nautilus
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

object RaceService {
    private val lock = ReentrantLock()

    /** Players currently racing in the active heat/final. */
    private val racePlayers = ObjectLinkedOpenHashSet<UUID>()

    /** Participants that have not raced in the current qualification stage yet. */
    private val waitingPlayers = ArrayDeque<UUID>()

    /** Players that advanced from heats and are waiting for the next stage/final. */
    private val qualifiedPlayers = ObjectArrayList<UUID>()

    /** Players that are out of the bracket but can keep watching. */
    private val eliminatedPlayers = ObjectLinkedOpenHashSet<UUID>()

    /** Players that are pure spectators from the base service or late joins. */
    private val spectators = ObjectLinkedOpenHashSet<UUID>()

    private val finalWinners = ObjectArrayList<UUID>()
    private val playerNautilus = ConcurrentHashMap<UUID, UUID>()

    @Volatile
    private var raceState = RaceState.DEACTIVATED

    @Volatile
    private var raceStage = RaceStage.NONE

    private val roundNumber = AtomicInteger(0)
    private val countdown = AtomicInteger(10)

    @Volatile
    private var countdownJob: Job? = null

    context(context: GameContext)
    suspend fun startSession() {
        countdownJob?.cancel("A new race session has been started.")
        ProgressService.clear()

        supervisorScope {
            for ((_, entityUuid) in playerNautilus) {
                val entity = Bukkit.getEntity(entityUuid) ?: continue
                launch(plugin.entityDispatcher(entity)) {
                    entity.remove()
                }
            }
        }

        playerNautilus.clear()

        lock.withLock {
            racePlayers.clear()
            waitingPlayers.clear()
            qualifiedPlayers.clear()
            eliminatedPlayers.clear()
            spectators.clear()
            finalWinners.clear()

            racePlayers.addAll(context.gamePlayers)
            waitingPlayers.addAll(context.reservePlayers)
            spectators.addAll(context.spectators)

            raceStage = if (waitingPlayers.isEmpty()) RaceStage.FINAL else RaceStage.QUALIFYING
            raceState = RaceState.LOBBY
            roundNumber.set(if (racePlayers.isEmpty()) 0 else 1)
        }

        context.onlineGamePlayers.forEach { player ->
            teleportToRoundLobby(player.uniqueId)
        }
        context.reservePlayers.forEach { uuid ->
            teleportToSpectatorLocation(uuid)
        }
        context.spectators.forEach { uuid ->
            teleportToSpectatorLocation(uuid)
        }
    }

    context(context: GameContext)
    fun addPlayer(uuid: UUID) {
        lock.withLock {
            removeFromCollections(uuid)
            racePlayers.add(uuid)
            if (raceStage == RaceStage.NONE) {
                raceStage = RaceStage.FINAL
            }
            if (raceState == RaceState.DEACTIVATED) {
                raceState = RaceState.LOBBY
            }
            roundNumber.getAndUpdate { number ->
                if (number == 0) {
                    1
                } else {
                    number
                }
            }
        }
        GameService.setParticipantRole(uuid, ParticipantRole.PLAYER)
        teleportToRoundLobby(uuid)
    }

    context(context: GameContext)
    fun addReserve(uuid: UUID) {
        lock.withLock {
            removeFromCollections(uuid)
            waitingPlayers.add(uuid)
            if (raceStage == RaceStage.NONE || raceStage == RaceStage.FINAL) {
                raceStage = RaceStage.QUALIFYING
            }
            if (raceState == RaceState.DEACTIVATED) {
                raceState = RaceState.LOBBY
            }
        }
        GameService.setParticipantRole(uuid, ParticipantRole.RESERVE)
        teleportToSpectatorLocation(uuid)
    }

    context(context: GameContext)
    fun addSpectator(uuid: UUID) {
        lock.withLock {
            removeFromCollections(uuid)
            spectators.add(uuid)
        }

        teleportToSpectatorLocation(uuid)
    }

    fun removePlayer(uuid: UUID) {
        removeParticipant(uuid, teleportToServerLobby = false)
    }

    fun removePlayer(player: Player, teleportToServerLobby: Boolean = true) {
        removeParticipant(player.uniqueId, teleportToServerLobby)
    }

    fun removeParticipant(uuid: UUID, teleportToServerLobby: Boolean = false) {
        lock.withLock {
            removeFromCollections(uuid)
        }

        removeNautilus(uuid)
        ProgressService.removePlayer(uuid)

        if (teleportToServerLobby) {
            teleportToServerLobby(uuid)
        }
    }

    context(context: GameContext)
    fun eliminatePlayer(player: Player) {
        eliminatePlayer(player.uniqueId)
    }

    context(context: GameContext)
    fun eliminatePlayer(uuid: UUID) {
        lock.withLock {
            removeFromCollections(uuid)
            eliminatedPlayers.add(uuid)
        }

        removeNautilus(uuid)
        ProgressService.removePlayer(uuid)
        GameService.setParticipantRole(uuid, ParticipantRole.SPECTATOR)
        teleportToSpectatorLocation(uuid)
    }

    fun isInRace(player: Player): Boolean = lock.withLock {
        player.uniqueId in racePlayers
    }

    fun getRacePlayers(): Set<UUID> = lock.withLock {
        racePlayers.toSet()
    }

    fun getWaitingPlayers(): List<UUID> = lock.withLock {
        waitingPlayers.toList()
    }

    fun getQualifiedPlayers(): List<UUID> = lock.withLock {
        qualifiedPlayers.toList()
    }

    fun getFinalWinners(): List<UUID> = lock.withLock {
        finalWinners.toList()
    }

    fun getSpectatorPlayers(): Set<UUID> = lock.withLock {
        buildSet {
            addAll(spectators)
            addAll(waitingPlayers)
            addAll(qualifiedPlayers)
            addAll(eliminatedPlayers)
            removeAll(racePlayers)
        }
    }

    fun removeSpectator(uuid: UUID, teleportToServerLobby: Boolean = true) {
        lock.withLock {
            spectators.remove(uuid)
            eliminatedPlayers.remove(uuid)
            waitingPlayers.remove(uuid)
            qualifiedPlayers.remove(uuid)
            finalWinners.remove(uuid)
        }

        if (teleportToServerLobby) {
            teleportToServerLobby(uuid)
        }
    }

    fun getRaceState(): RaceState = raceState

    fun setRaceState(state: RaceState) {
        raceState = state
    }

    fun getRaceStage(): RaceStage = raceStage

    fun getRoundNumber(): Int = roundNumber.get()

    fun setPlayerOnNautilus(player: Player) {
        removeNautilus(player.uniqueId)

        val location = player.location
        val nautilus = location.world.spawn(location, Nautilus::class.java) { entity ->
            entity.inventory.addItem(ItemStack(Material.SADDLE))
            entity.owner = player
            entity.isInvulnerable = true
        }

        playerNautilus[player.uniqueId] = nautilus.uniqueId
        nautilus.addPassenger(player)
    }

    fun startCountdown() {
        if (raceState != RaceState.COUNTDOWN) return
        if (countdownJob != null) return

        countdown.set(10)

        countdownJob = plugin.scope.runAtFixedRate(1.seconds) {
            val currentCountdown = countdown.getAndDecrement()
            if (currentCountdown <= 0) {
                setRaceState(RaceState.RUNNING)
                plugin.launch {
                    RegionService.fillBlocks(Material.AIR)
                }
                getRacePlayers().forEach { ProgressService.addPlayer(it) }

                countdownJob = null
                cancel("Countdown has finished.")
                return@runAtFixedRate
            }

            val titleTargets = getRacePlayers() + getSpectatorPlayers()
            titleTargets.forEach { uuid ->
                Bukkit.getPlayer(uuid)?.let { showTitle(it) }
            }
        }
    }

    context(context: GameContext)
    fun nextRound(advanceCount: Int): RaceRoundAdvanceResult {
        val currentState = raceState
        if (currentState == RaceState.DEACTIVATED) {
            return RaceRoundAdvanceResult(RaceRoundAdvanceType.NO_ACTIVE_RACE, raceStage, roundNumber.get())
        }
        if (currentState != RaceState.RUNNING) {
            return RaceRoundAdvanceResult(RaceRoundAdvanceType.RACE_NOT_RUNNING, raceStage, roundNumber.get())
        }

        val activeBefore = getRacePlayers().toList()
        if (activeBefore.isEmpty()) {
            return RaceRoundAdvanceResult(RaceRoundAdvanceType.NO_ACTIVE_RACERS, raceStage, roundNumber.get())
        }

        val ranked = rankActivePlayers(activeBefore)
        val effectiveAdvanceCount = advanceCount.coerceIn(1, activeBefore.size)
        val advanced = ranked.take(effectiveAdvanceCount)
        val advancedSet = advanced.toSet()
        val eliminated = activeBefore.filter { it !in advancedSet }
        val stageBefore = raceStage

        eliminated.forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendText {
                appendInfoPrefix()
                info("Du bist leider raus, danke fürs Mitmachen!")
            }
            eliminatePlayer(uuid)
        }

        advanced.forEach { uuid ->
            removeNautilus(uuid)
            ProgressService.resetProgress(uuid)
            Bukkit.getPlayer(uuid)?.sendText {
                appendInfoPrefix()
                if (stageBefore == RaceStage.FINAL) {
                    info("Du bist in den Top")
                    appendSpace()
                    variableValue(advanced.size.toString())
                    appendSpace()
                    info("gelandet!")
                } else {
                    info("Du hast es in die nächste Runde geschafft!")
                }
            }
        }

        return when (stageBefore) {
            RaceStage.FINAL -> finishFinal(advanced, eliminated)
            RaceStage.QUALIFYING -> finishQualificationHeat(advanced, eliminated)
            RaceStage.NONE,
            RaceStage.FINISHED -> RaceRoundAdvanceResult(
                RaceRoundAdvanceType.NO_ACTIVE_RACE,
                stageBefore,
                roundNumber.get(),
                advanced,
                eliminated
            )
        }
    }

    fun playerToStartMid() {
        setRaceState(RaceState.WAITING)

        val starts = RaceConfig.getConfig().starts
        if (starts.isEmpty()) return

        val active = getRacePlayers().toList()
        active.forEach { uuid ->
            GameService.setParticipantRole(uuid, ParticipantRole.PLAYER)
            ProgressService.resetProgress(uuid)
        }

        val context = GameService.snapshot() ?: error("Could not get current game context")

        active
            .mapNotNull { Bukkit.getPlayer(it) }
            .forEachIndexed { index, player ->
                val start = starts.getOrNull(index) ?: starts.first()
                val location = midStartLocation(context, start)

                plugin.launch {
                    player.teleportAsync(location).await()
                    withContext(plugin.entityDispatcher(player)) {
                        setPlayerOnNautilus(player)
                    }
                }
            }
    }

    suspend fun stopRace(notifyPlayers: Boolean = true) {
        countdownJob?.cancel("Race has been stopped.")

        val playersToClean = lock.withLock {
            buildSet {
                addAll(racePlayers)
                addAll(waitingPlayers)
                addAll(qualifiedPlayers)
                addAll(eliminatedPlayers)
                addAll(spectators)
                addAll(finalWinners)
            }.also {
                racePlayers.clear()
                waitingPlayers.clear()
                qualifiedPlayers.clear()
                eliminatedPlayers.clear()
                spectators.clear()
                finalWinners.clear()
                raceStage = RaceStage.NONE
                raceState = RaceState.DEACTIVATED
                roundNumber.set(0)
            }
        }

        ProgressService.clear()
        RegionService.fillBlocks(Material.AIR)

        playersToClean.forEach { uuid ->
            removeNautilus(uuid)
            val player = Bukkit.getPlayer(uuid)
            if (player != null) {
                if (notifyPlayers) {
                    player.sendText {
                        appendInfoPrefix()
                        info("Das Rennen wurde gestoppt.")
                    }
                }
                GameService.teleportToServerLobby(player)
            }
        }

        playerNautilus.clear()
    }

    context(context: GameContext)
    private fun finishQualificationHeat(
        advanced: List<UUID>,
        eliminated: List<UUID>
    ): RaceRoundAdvanceResult {
        val preparation = lock.withLock {
            racePlayers.clear()

            advanced.forEach { uuid ->
                if (uuid !in qualifiedPlayers) {
                    qualifiedPlayers.add(uuid)
                }
            }

            if (waitingPlayers.isNotEmpty()) {
                val next = pollNextBatch()
                racePlayers.addAll(next)
                roundNumber.getAndIncrement()
                raceState = RaceState.WAITING
                RoundPreparation(RaceRoundAdvanceType.NEXT_HEAT_PREPARED, RaceStage.QUALIFYING, next)
            } else if (qualifiedPlayers.isEmpty) {
                raceStage = RaceStage.FINISHED
                raceState = RaceState.FINISHED
                RoundPreparation(RaceRoundAdvanceType.FINISHED, RaceStage.FINISHED, emptyList())
            } else if (qualifiedPlayers.size <= configuredPlayersPerRound()) {
                val finalists = qualifiedPlayers.toList()
                qualifiedPlayers.clear()
                racePlayers.addAll(finalists)
                raceStage = RaceStage.FINAL
                roundNumber.getAndIncrement()
                raceState = RaceState.WAITING
                RoundPreparation(RaceRoundAdvanceType.FINAL_PREPARED, RaceStage.FINAL, finalists)
            } else {
                val nextStagePlayers = qualifiedPlayers.toList()
                qualifiedPlayers.clear()
                waitingPlayers.addAll(nextStagePlayers)
                val next = pollNextBatch()
                racePlayers.addAll(next)
                roundNumber.getAndIncrement()
                raceState = RaceState.WAITING
                RoundPreparation(RaceRoundAdvanceType.NEXT_STAGE_PREPARED, RaceStage.QUALIFYING, next)
            }
        }

        val nextRacerSet = preparation.nextRacers.toSet()

        advanced
            .filter { it !in nextRacerSet }
            .forEach { uuid ->
                GameService.setParticipantRole(uuid, ParticipantRole.RESERVE)
                teleportToSpectatorLocation(uuid)
            }

        preparation.nextRacers.forEach { uuid ->
            GameService.setParticipantRole(uuid, ParticipantRole.PLAYER)
        }

        if (preparation.nextRacers.isNotEmpty()) {
            playerToStartMid()
        }

        return RaceRoundAdvanceResult(
            type = preparation.type,
            stage = preparation.stage,
            roundNumber = roundNumber.get(),
            advanced = advanced,
            eliminated = eliminated,
            nextRacers = preparation.nextRacers
        )
    }

    context(context: GameContext)
    private fun finishFinal(
        winners: List<UUID>,
        eliminated: List<UUID>
    ): RaceRoundAdvanceResult {
        lock.withLock {
            racePlayers.clear()
            finalWinners.clear()
            finalWinners.addAll(winners)
            winners.forEach { uuid ->
                spectators.add(uuid)
            }
            raceStage = RaceStage.FINISHED
            raceState = RaceState.FINISHED
        }

        winners.forEachIndexed { index, uuid ->
            removeNautilus(uuid)
            ProgressService.resetProgress(uuid)
            GameService.setParticipantRole(uuid, ParticipantRole.SPECTATOR)
            teleportToSpectatorLocation(uuid)
            Bukkit.getPlayer(uuid)?.sendText {
                appendSuccessPrefix()
                success("Du hast Platz")
                appendSpace()
                variableValue("#${index + 1}")
                appendSpace()
                success("im Race Event erreicht!")
            }
        }

        broadcastFinalWinners(winners)

        return RaceRoundAdvanceResult(
            type = RaceRoundAdvanceType.FINISHED,
            stage = RaceStage.FINISHED,
            roundNumber = roundNumber.get(),
            advanced = winners,
            eliminated = eliminated,
            nextRacers = emptyList()
        )
    }

    private fun rankActivePlayers(active: List<UUID>): List<UUID> {
        val activeSet = active.toSet()
        val finished = ProgressService.getPlaceList().filter { it in activeSet }
        val unfinished = active.filter { it !in finished }
        return finished + unfinished
    }

    private fun pollNextBatch(): List<UUID> {
        val size = configuredPlayersPerRound()
        val next = mutableListOf<UUID>()

        while (next.size < size && waitingPlayers.isNotEmpty()) {
            next.add(waitingPlayers.removeFirst())
        }

        return next
    }

    private fun configuredPlayersPerRound(): Int {
        return RaceConfig.getConfig().gameplay.playersPerRound.coerceAtLeast(1)
    }

    private fun removeFromCollections(uuid: UUID) {
        racePlayers.remove(uuid)
        waitingPlayers.remove(uuid)
        qualifiedPlayers.remove(uuid)
        eliminatedPlayers.remove(uuid)
        spectators.remove(uuid)
        finalWinners.remove(uuid)
    }

    context(context: GameContext)
    private fun teleportToRoundLobby(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        player.teleportAsync(RaceConfig.getConfig().roundLobbyLocation.toLocation())
    }

    context(context: GameContext)
    private fun teleportToSpectatorLocation(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        plugin.launch {
            player.teleportAsync(RaceConfig.getConfig().spectatorLocation.toLocation())
        }
    }

    private fun teleportToServerLobby(uuid: UUID) {
        val player = Bukkit.getPlayer(uuid) ?: return
        plugin.launch {
            GameService.teleportToServerLobby(player)
        }
    }

    private fun removeNautilus(uuid: UUID) {
        playerNautilus.remove(uuid)?.let { nautilusId ->
            Bukkit.getEntity(nautilusId)?.remove()
        }
    }


    private fun midStartLocation(context: GameContext, start: RaceConfig.StartConfig): Location {
//        val midX = (start.x1 + start.x2) / 2
//        val midY = (start.y1 + start.y2) / 2
//        val midZ = (start.z1 + start.z2) / 2

        return start.center.toLocation(context.eventWorld).setRotation(start.yaw, start.pitch)
    }

    private fun showTitle(player: Player) {
        plugin.launch {
            withContext(plugin.entityDispatcher(player)) {
                player.showTitle(
                    title {
                        title {
                            text(
                                if (countdown.get() <= 0) "LOS!" else countdown.toString(),
                                Colors.VARIABLE_VALUE
                            )
                        }
                        times {
                            fadeIn(0)
                            stay(20)
                            fadeOut(0)
                        }
                    }
                )
            }
        }
    }

    private fun broadcastFinalWinners(winners: List<UUID>) {
        (getRacePlayers() + getSpectatorPlayers() + getFinalWinners()).distinct().forEach { uuid ->
            Bukkit.getPlayer(uuid)?.sendText {
                appendSuccessPrefix()
                success("Das Race Event ist beendet.")
                if (winners.isNotEmpty()) {
                    appendNewline()
                    info("Gewinner:")
                    winners.forEachIndexed { index, winnerUuid ->
                        appendNewline()
                        variableValue("#${index + 1}")
                        appendSpace()
                        variableValue(Bukkit.getOfflinePlayer(winnerUuid).name ?: winnerUuid.toString())
                    }
                }
            }
        }
    }

    private data class RoundPreparation(
        val type: RaceRoundAdvanceType,
        val stage: RaceStage,
        val nextRacers: List<UUID>
    )
}

data class RaceRoundAdvanceResult(
    val type: RaceRoundAdvanceType,
    val stage: RaceStage,
    val roundNumber: Int,
    val advanced: List<UUID> = emptyList(),
    val eliminated: List<UUID> = emptyList(),
    val nextRacers: List<UUID> = emptyList()
)

enum class RaceRoundAdvanceType {
    NO_ACTIVE_RACE,
    RACE_NOT_RUNNING,
    NO_ACTIVE_RACERS,
    NEXT_HEAT_PREPARED,
    NEXT_STAGE_PREPARED,
    FINAL_PREPARED,
    FINISHED
}
