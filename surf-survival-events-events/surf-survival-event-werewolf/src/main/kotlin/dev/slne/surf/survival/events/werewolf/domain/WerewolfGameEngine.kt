package dev.slne.surf.survival.events.werewolf.domain

import dev.slne.surf.survival.events.werewolf.domain.roleActions.*
import dev.slne.surf.survival.events.werewolf.messaging.WerewolfMessenger
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.*
import dev.slne.surf.survival.events.werewolf.domain.roleActions.AmorActions
import dev.slne.surf.survival.events.werewolf.plugin
import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class WerewolfGameEngine(
    private val service: WerewolfService
) {
    private var roundState = GameRoundState.initial()

    private val messenger = WerewolfMessenger(service)
    private fun nightResolver() = NightResolver(
        service.players,
        roundState.dayNumber,
        roundState.werewolfTarget,
        currentSerialKillerTargetLocked()
    )
    private fun nightStepCoordinator() = NightStepCoordinator(service.players, roundState.dayNumber)

    val currentPhase: GameState
        get() = synchronized(service.lock) { roundState.phase }

    val phaseRemainingSeconds: Duration
        get() = synchronized(service.lock) { roundState.phaseRemainingSeconds }

    val currentNightStep: NightStep?
        get() = synchronized(service.lock) { roundState.nightStep }

    val currentDayNumber: Int
        get() = synchronized(service.lock) { roundState.dayNumber }

    val currentWerewolfTarget: UUID?
        get() = synchronized(service.lock) { roundState.werewolfTarget }

    val currentSerialKillerTarget: UUID?
        get() = synchronized(service.lock) { currentSerialKillerTargetLocked() }

    val currentWitchHealTargets: Set<UUID>
        get() = synchronized(service.lock) {
            setOfNotNull(roundState.werewolfTarget, currentSerialKillerTargetLocked())
                .filter { targetId -> service.players[targetId]?.isAlive == true }
                .toSet()
        }

    private fun currentSerialKillerTargetLocked(): UUID? =
        SerialKillerActions.resolveTarget(roundState.nightActions)

    fun startGameEngine(): PhaseAdvanceResult = synchronized(service.lock) {
        roundState = GameRoundState(
            phase = GameState.DAY,
            dayNumber = 1,
            nightStep = null,
            nightActions = mutableListOf(),
            votes = mutableMapOf(),
            phaseRemainingSeconds = GameState.DAY.time,
        )

        PhaseAdvanceResult(nextPhase = roundState.phase)
    }

    fun tick(): PhaseAdvanceResult? = synchronized(service.lock) {
        if (roundState.phaseRemainingSeconds <= 1.seconds) {
            roundState = roundState.copy(phaseRemainingSeconds = 0.seconds)

            if (roundState.phase == GameState.NIGHT &&
                roundState.nightStep != null &&
                roundState.nightStep != NightStep.RESOLVE
            ) {
                advanceNightStepOnTimeout()
                return@synchronized null
            }

            return@synchronized advancePhaseLocked()
        }

        roundState = roundState.copy(
            phaseRemainingSeconds = roundState.phaseRemainingSeconds - 1.seconds,
        )

        plugin.logger.fine(
            "Werewolf tick state: remaining=${roundState.phaseRemainingSeconds}, " +
                    "phase=${roundState.phase}, " +
                    "nightStep=${roundState.nightStep}, " +
                    "nightStepTime=${roundState.nightStep?.time}"
        )

        null
    }

    fun removePlayer(playerId: UUID): Unit = synchronized(service.lock) {
        roundState = roundState.copy(
            nightActions = roundState.nightActions
                .filterNot { actionReferencesPlayer(it, playerId) }
                .toMutableList(),
            votes = roundState.votes
                .filterKeys { it != playerId }
                .filterValues { it != playerId }
                .toMutableMap(),
            protectedPlayer = roundState.protectedPlayer.takeUnless { it == playerId },
            werewolfTarget = roundState.werewolfTarget.takeUnless { it == playerId },
            mayorPlayer = roundState.mayorPlayer.takeUnless { it == playerId },
            mayorVoteRequired = roundState.mayorVoteRequired || (roundState.mayorPlayer == playerId),
            mayorVotes = roundState.mayorVotes
                .filterKeys { it != playerId }
                .filterValues { it != playerId }
                .toMutableMap()
        )

        if (roundState.phase == GameState.NIGHT) {
            advanceNightStepIfReady()
        }
    }

    fun advancePhase(): PhaseAdvanceResult = synchronized(service.lock) { advancePhaseLocked() }

    private fun advancePhaseLocked(): PhaseAdvanceResult = when (roundState.phase) {
            GameState.MAYOR_VOTE -> advanceMayorVotePhase()
            GameState.DAY -> advanceDayPhase()
            GameState.VOTE -> advanceVotePhase()
            GameState.NIGHT -> advanceNightPhase()
        }

    private fun advanceMayorVotePhase(): PhaseAdvanceResult {
        val standings = calculateMayorVoteStandings()
        val electedMayor = resolveMayorVote()
        roundState = roundState.copy(
            mayorPlayer = electedMayor,
            mayorVoteRequired = false
        )

        messenger.announceVotings(roundState.phase, standings, electedMayor)
        beginNightPhaseLocked()

        return PhaseAdvanceResult(
            nextPhase = roundState.phase,
            electedMayor = electedMayor,
            voteStandings = standings
        )
    }

    private fun advanceDayPhase(): PhaseAdvanceResult {
        clearDeadMayorIfNeeded()

        if (roundState.mayorVoteRequired) {
            beginMayorVotingLocked()
        } else {
            beginVotePhaseLocked()
        }

        return PhaseAdvanceResult(nextPhase = roundState.phase)
    }

    private fun advanceVotePhase(): PhaseAdvanceResult {
        val standings = calculateVoteStandings()
        val votedOutPlayer = resolveUniqueVoteWinner(standings)

        messenger.announceVotings(
            state = roundState.phase,
            standings = standings,
            eliminatedPlayers = listOfNotNull(votedOutPlayer)
        )

        if (votedOutPlayer != null) {
            service.executePlayer(votedOutPlayer)
        }

        val winner = checkWinConditionLocked()
        if (winner != null) {
            return PhaseAdvanceResult(
                nextPhase = roundState.phase,
                winner = winner,
                eliminatedPlayers = listOfNotNull(votedOutPlayer),
                voteStandings = standings
            )
        }

        beginNightPhaseLocked()
        return PhaseAdvanceResult(
            nextPhase = roundState.phase,
            eliminatedPlayers = listOfNotNull(votedOutPlayer),
            voteStandings = standings
        )
    }

    private fun advanceNightPhase(): PhaseAdvanceResult {
        val nightResolution = resolveNightLocked()
        val winner = checkWinConditionLocked()

        if (winner != null) {
            return PhaseAdvanceResult(
                nextPhase = roundState.phase,
                winner = winner,
                eliminatedPlayers = nightResolution.eliminatedPlayers
            )
        }

        beginDayPhaseLocked(increaseDayNumber = true)
        return PhaseAdvanceResult(
            nextPhase = roundState.phase,
            eliminatedPlayers = nightResolution.eliminatedPlayers
        )
    }

    fun beginMayorVoting(): Unit = synchronized(service.lock) { beginMayorVotingLocked() }

    private fun beginMayorVotingLocked() {
        roundState = roundState.copy(
            phase = GameState.MAYOR_VOTE,
            phaseRemainingSeconds = GameState.MAYOR_VOTE.time,
            mayorVotes = mutableMapOf(),
            nightStep = null
        )

        messenger.announceMayorVotingStarted()
        service.setGameState(GameState.MAYOR_VOTE)
    }

    fun beginVotePhase(): Unit = synchronized(service.lock) { beginVotePhaseLocked() }

    private fun beginVotePhaseLocked() {
        roundState = roundState.copy(
            phase = GameState.VOTE,
            phaseRemainingSeconds = GameState.VOTE.time,
            votes = mutableMapOf(),
            nightStep = null
        )

        messenger.announceVillageVoteStarted()
        service.setGameState(GameState.VOTE)
    }

    fun beginNightPhase(): Unit = synchronized(service.lock) { beginNightPhaseLocked() }

    private fun beginNightPhaseLocked() {
        roundState = roundState.copy(
            phase = GameState.NIGHT,
            phaseRemainingSeconds = GameState.NIGHT.time,
            nightStep = null,
            protectedPlayer = null,
            werewolfTarget = null,
            nightActions = mutableListOf()
        )

        setNightStep(nightStepCoordinator().firstStep(), announce = false)
        service.setGameState(GameState.NIGHT)
    }

    fun beginDayPhase(increaseDayNumber: Boolean = true): Unit =
        synchronized(service.lock) { beginDayPhaseLocked(increaseDayNumber) }

    private fun beginDayPhaseLocked(increaseDayNumber: Boolean = true) {
        roundState = roundState.copy(
            phase = GameState.DAY,
            phaseRemainingSeconds = GameState.DAY.time,
            dayNumber = if (increaseDayNumber) roundState.dayNumber + 1 else roundState.dayNumber,
            nightStep = null,
            votes = mutableMapOf(),
            protectedPlayer = null,
            werewolfTarget = null,
            nightActions = mutableListOf()
        )

        service.syncWerewolfPrivateChannel(null)
        service.setGameState(GameState.DAY)
    }

    fun submitMayorVote(voter: UUID, target: UUID): Boolean = synchronized(service.lock) {
        submitVote(
            expectedPhase = GameState.MAYOR_VOTE,
            votes = roundState.mayorVotes,
            voter = voter,
            target = target
        )
    }

    fun resolveMayorVote(): UUID? = synchronized(service.lock) {
        if (roundState.phase != GameState.MAYOR_VOTE) return@synchronized null
        resolveMayorVoteWinner(calculateMayorVoteStandings())
    }

    fun submitVote(voter: UUID, target: UUID): Boolean = synchronized(service.lock) {
        submitVote(
            expectedPhase = GameState.VOTE,
            votes = roundState.votes,
            voter = voter,
            target = target
        )
    }

    fun resolveVote(): UUID? = synchronized(service.lock) {
        if (roundState.phase != GameState.VOTE) return@synchronized null
        val killed = resolveUniqueVoteWinner(calculateVoteStandings()) ?: return@synchronized null
        service.executePlayer(killed)
        killed
    }

    private fun resolveUniqueVoteWinner(standings: List<VoteStanding>): UUID? {
        val topStanding = standings.firstOrNull() ?: return null
        val topTieCount = standings.count { it.votes == topStanding.votes }

        return topStanding.target.takeIf { topTieCount == 1 }
    }

    private fun resolveMayorVoteWinner(standings: List<VoteStanding>): UUID? {
        val uniqueWinner = resolveUniqueVoteWinner(standings)
        if (uniqueWinner != null) return uniqueWinner

        val topVotes = standings.firstOrNull()?.votes ?: return null
        return standings
            .filter { it.votes == topVotes }
            .randomOrNull()
            ?.target
    }

    private fun submitVote(
        expectedPhase: GameState,
        votes: MutableMap<UUID, UUID>,
        voter: UUID,
        target: UUID,
    ): Boolean {
        if (roundState.phase != expectedPhase) return false
        if (service.players[voter]?.isAlive != true) return false
        if (service.players[target]?.isAlive != true) return false
        val previousTarget = votes[voter]
        votes[voter] = target

        if (previousTarget != target) {
            messenger.announceLeaderVoteSubmitted(expectedPhase, voter, target, previousTarget)
        }

        return true
    }

    private fun calculateMayorVoteStandings(): List<VoteStanding> {
        if (roundState.phase != GameState.MAYOR_VOTE) return emptyList()

        return VoteResolver.calculateStandings(
            players = service.players,
            votes = roundState.mayorVotes
        ) { 1 }
    }

    private fun calculateVoteStandings(): List<VoteStanding> {
        if (roundState.phase != GameState.VOTE) return emptyList()

        val mayorPlayer = roundState.mayorPlayer?.let {
            service.players[it]
        }?.takeIf { it.isAlive }

        return VoteResolver.calculateStandings(
            players = service.players,
            votes = roundState.votes
        ) { voterPlayer ->
            if (voterPlayer == mayorPlayer) 2 else 1
        }
    }

    private fun clearDeadMayorIfNeeded() {
        val mayorId = roundState.mayorPlayer ?: return
        if (service.players[mayorId]?.isAlive == true) return

        roundState = roundState.copy(
            mayorPlayer = null,
            mayorVoteRequired = true
        )
    }

    fun submitNightAction(action: NightAction): Boolean = synchronized(service.lock) {
        if (roundState.phase != GameState.NIGHT) return@synchronized false
        val actor = service.players[action.actor] ?: return@synchronized false
        if (!actor.isAlive) return@synchronized false
        if (!nightStepCoordinator().isActionAllowed(roundState.nightStep, action)) return@synchronized false
        if (!nightResolver().isValid(action, actor.role)) return@synchronized false

        val previousAction = replaceNightAction(action)
        if (previousAction != action &&
            action !is NightAction.GirlPeek &&
            action !is NightAction.SeerInspect
        ) {
            messenger.announceLeaderNightAction(action, previousAction)
        }

        advanceNightStepIfReady()
        service.refreshCommandRequirements()

        true
    }

    fun inspectWithSeer(actor: UUID, target: UUID): WerwolfRoles? = synchronized(service.lock) {
        val action = NightAction.SeerInspect(actor = actor, target = target)
        if (!submitNightAction(action)) return@synchronized null

        SeerActions.inspectTarget(action, service.players)?.also { inspectedRole ->
            messenger.announceLeaderSeerInspection(actor, target, inspectedRole)
        }
    }

    fun peekWithGirl(actor: UUID): GirlPeekOutcome? = synchronized(service.lock) {
        if (roundState.phase != GameState.NIGHT) return@synchronized null
        if (roundState.nightStep != NightStep.GIRL) return@synchronized null
        if (service.players[actor]?.role != WerwolfRoles.GIRL) return@synchronized null
        if (service.players[actor]?.isAlive != true) return@synchronized null

        val outcome = GirlActions.rollOutcome(service.players)
        val action = NightAction.GirlPeek(actor = actor, outcome = outcome)
        if (!submitNightAction(action)) return@synchronized null

        messenger.announceLeaderGirlPeek(actor, outcome)
        outcome
    }

    fun usePriestHolyWater(actor: UUID, target: UUID): PriestActionResult = synchronized(service.lock) {
        if (roundState.phase != GameState.DAY) return@synchronized PriestActionResult.WrongPhase

        val actorPlayer = service.players[actor] ?: return@synchronized PriestActionResult.InvalidActor
        val targetPlayer = service.players[target] ?: return@synchronized PriestActionResult.InvalidTarget

        if (actorPlayer.role != WerwolfRoles.PRIEST || !actorPlayer.isAlive) {
            return@synchronized PriestActionResult.InvalidActor
        }

        if (!actorPlayer.hasPriestHolyWater) {
            return@synchronized PriestActionResult.AlreadyUsed
        }

        if (!PriestActions.isValid(actorPlayer, targetPlayer)) {
            return@synchronized PriestActionResult.InvalidTarget
        }

        actorPlayer.hasPriestHolyWater = false

        val resolution = PriestActions.resolve(actor, target, service.players)
        messenger.announcePriestHolyWater(actor, target, resolution.hitWerewolf)
        resolution.eliminatedPlayers.forEach(service::executePlayer)
        service.refreshCommandRequirements()

        PriestActionResult.Success(
            hitWerewolf = resolution.hitWerewolf,
            eliminatedPlayers = resolution.eliminatedPlayers,
            winner = checkWinConditionLocked()
        )
    }

    fun resolveNight(): NightResolutionResult = synchronized(service.lock) { resolveNightLocked() }

    private fun resolveNightLocked(): NightResolutionResult {
        if (roundState.phase != GameState.NIGHT) return NightResolutionResult()

        val doctorProtectedPlayer = DoctorActions.resolveTarget(roundState.nightActions)
        val witchHealTarget = WitchActions.resolveHealTarget(roundState.nightActions)
        val witchPoisonTarget = WitchActions.resolvePoisonTarget(roundState.nightActions)
        val serialKillerTarget = SerialKillerActions.resolveTarget(roundState.nightActions)
        val caughtGirls = GirlActions.resolveCaughtGirls(roundState.nightActions)
        val resolution = nightResolver().resolve(roundState.nightActions)
        AmorActions.apply(service.players, resolution.lovers)
        WitchActions.apply(service.players, roundState.nightActions)
        messenger.announceLovers(resolution.lovers)
        messenger.announceLeaderNightResolved(
            resolution = resolution,
            doctorProtectedPlayer = doctorProtectedPlayer,
            witchHealTarget = witchHealTarget,
            witchPoisonTarget = witchPoisonTarget,
            serialKillerTarget = serialKillerTarget,
            caughtGirls = caughtGirls
        )
        resolution.eliminatedPlayers.forEach(service::executePlayer)

        roundState = roundState.copy(
            protectedPlayer = resolution.protectedPlayer,
            werewolfTarget = resolution.werewolfTarget,
            nightActions = mutableListOf()
        )

        return resolution
    }

    fun announceCurrentNightStep(): Unit = synchronized(service.lock) {
        messenger.announceNightStep(roundState.nightStep)
    }

    fun canRoleActAtNight(role: WerwolfRoles): Boolean = synchronized(service.lock) {
        if (roundState.phase != GameState.NIGHT) return@synchronized false
        roundState.nightStep?.activeRole == role
    }

    fun getWerewolfTargetFromLineOfSight(player: Player): UUID? = synchronized(service.lock) {
        if (roundState.phase != GameState.NIGHT) return@synchronized null
        if (service.getPlayerRole(player.uniqueId) != WerwolfRoles.WERWOLF) return@synchronized null
        if (service.players[player.uniqueId]?.isAlive != true) return@synchronized null

        val targetPlayer = player.getTargetEntity(50, true) as? Player ?: return@synchronized null
        val targetId = targetPlayer.uniqueId

        if (service.players[targetId]?.isAlive != true) return@synchronized null

        targetId
    }

    fun getWitchHealTargets(player: Player): Set<UUID> = synchronized(service.lock) {
        if (roundState.phase != GameState.NIGHT) return@synchronized emptySet()
        if (roundState.nightStep != NightStep.WITCH) return@synchronized emptySet()
        if (service.getPlayerRole(player.uniqueId) != WerwolfRoles.WITCH) return@synchronized emptySet()
        if (service.players[player.uniqueId]?.isAlive != true) return@synchronized emptySet()

        setOfNotNull(roundState.werewolfTarget, currentSerialKillerTargetLocked())
            .filter { targetId -> service.players[targetId]?.isAlive == true }
            .toSet()
    }

    fun checkWinCondition(): GameOutcome? = synchronized(service.lock) { checkWinConditionLocked() }

    private fun checkWinConditionLocked(): GameOutcome? {
        val alivePlayers = service.players.values.filter { it.isAlive }
        if (hasAliveLoverPair(alivePlayers)) return GameOutcome.LoversWin

        val aliveSerialKillers = alivePlayers.count { it.role == WerwolfRoles.SERIAL_KILLER }
        if (aliveSerialKillers > 0) {
            if (alivePlayers.size == aliveSerialKillers) return GameOutcome.SerialKillerWin
            return null
        }

        val aliveWerewolves = alivePlayers.count { it.role == WerwolfRoles.WERWOLF }
        val aliveVillagers = alivePlayers.count { it.role != WerwolfRoles.WERWOLF }

        if (aliveWerewolves == 0) return GameOutcome.VillagersWin
        if (aliveWerewolves >= aliveVillagers) return GameOutcome.WerewolvesWin

        return null
    }

    private fun hasAliveLoverPair(alivePlayers: List<WerewolfPlayer>): Boolean {
        if (alivePlayers.size != 2) return false

        val firstPlayer = alivePlayers[0]
        val secondPlayer = alivePlayers[1]

        return firstPlayer.inLoveWith == secondPlayer.uuid &&
                secondPlayer.inLoveWith == firstPlayer.uuid
    }

    private fun replaceNightAction(newAction: NightAction): NightAction? {
        val previousAction = roundState.nightActions.lastOrNull { existingAction ->
            isSameNightActionSlot(existingAction, newAction)
        }

        roundState.nightActions.removeAll { existingAction ->
            isSameNightActionSlot(existingAction, newAction)
        }
        roundState.nightActions.add(newAction)
        return previousAction
    }

    private fun advanceNightStepIfReady() {
        if (roundState.nightStep == NightStep.WEREWOLVES) {
            resolveWerewolfTargetForCurrentStep()
        }

        val nextStep = nightStepCoordinator().nextStep(
            currentStep = roundState.nightStep,
            actions = roundState.nightActions
        ) ?: return

        setNightStep(nextStep)
    }

    private fun advanceNightStepOnTimeout(
        cause: NightStepAdvanceCause = NightStepAdvanceCause.TIMEOUT,
        skippedBy: UUID? = null,
    ) {
        val currentStep = roundState.nightStep ?: return
        if (currentStep == NightStep.WEREWOLVES) {
            resolveWerewolfTargetForCurrentStep()
        }

        val nextStep = nightStepCoordinator().nextStepAfterTimeout(currentStep)
            ?: NightStep.RESOLVE

        when (cause) {
            NightStepAdvanceCause.SKIPPED -> messenger.announceLeaderNightStepSkipped(currentStep, nextStep, skippedBy)
            NightStepAdvanceCause.TIMEOUT -> messenger.announceLeaderNightStepTimeout(currentStep, nextStep)
        }

        setNightStep(nextStep)
    }

    private fun setNightStep(step: NightStep, announce: Boolean = true) {
        roundState = roundState.copy(
            nightStep = step,
            phaseRemainingSeconds = step.time
        )

        service.syncWerewolfPrivateChannel(step)

        if (announce) {
            messenger.announceNightStep(step)
        }
    }

    private fun isSameNightActionSlot(existingAction: NightAction, newAction: NightAction): Boolean =
        existingAction.actor == newAction.actor &&
                existingAction::class == newAction::class

    fun skipCurrentNightStep(skippedBy: UUID? = null): Boolean = synchronized(service.lock) {
        if (roundState.phase != GameState.NIGHT) return@synchronized false
        val currentStep = roundState.nightStep ?: return@synchronized false
        if (currentStep == NightStep.RESOLVE) return@synchronized false

        advanceNightStepOnTimeout(NightStepAdvanceCause.SKIPPED, skippedBy)
        service.refreshCommandRequirements()
        true
    }

    private fun resolveWerewolfTargetForCurrentStep() {
        val werewolfActions = roundState.nightActions.filterIsInstance<NightAction.WerewolfKill>()
        val resolvedTarget = nightResolver().resolveWerewolfTarget(roundState.nightActions)

        roundState = roundState.copy(werewolfTarget = resolvedTarget)
        messenger.announceLeaderWerewolfTargetResolved(resolvedTarget, werewolfActions)
    }

    private fun actionReferencesPlayer(action: NightAction, playerId: UUID): Boolean {
        if (action.actor == playerId) return true

        return when (action) {
            is NightAction.AmorLink -> action.first == playerId || action.second == playerId
            is NightAction.DoctorProtect -> action.target == playerId
            is NightAction.GirlPeek -> (action.outcome as? GirlPeekOutcome.FoundWerewolf)?.target == playerId
            is NightAction.SeerInspect -> action.target == playerId
            is NightAction.SerialKillerKill -> action.target == playerId
            is NightAction.WerewolfKill -> action.target == playerId
            is NightAction.WitchHeal -> action.target == playerId
            is NightAction.WitchPoison -> action.target == playerId
        }
    }

    private enum class NightStepAdvanceCause {
        TIMEOUT,
        SKIPPED
    }
}
