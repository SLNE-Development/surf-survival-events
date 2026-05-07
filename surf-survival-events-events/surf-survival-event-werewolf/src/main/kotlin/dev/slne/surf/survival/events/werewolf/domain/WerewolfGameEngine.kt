package dev.slne.surf.event.werewolf.domain

import dev.slne.surf.event.werewolf.domain.roleActions.*
import dev.slne.surf.event.werewolf.messaging.WerewolfMessenger
import dev.slne.surf.event.werewolf.service.WerewolfService
import dev.slne.surf.event.werewolf.util.*
import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WerewolfGameEngine(
    private val service: WerewolfService
) {
    private var roundState = GameRoundState.initial()

    private val messenger = WerewolfMessenger(service)
    private fun nightResolver() = NightResolver(service.players, roundState.dayNumber, roundState.werewolfTarget)
    private fun nightStepCoordinator() = NightStepCoordinator(service.players, roundState.dayNumber)

    val currentPhase: GameState
        get() = roundState.phase

    val phaseRemainingSeconds: Duration
        get() = roundState.phaseRemainingSeconds

    val currentNightStep: NightStep?
        get() = roundState.nightStep

    fun startGameEngine(): PhaseAdvanceResult {
        roundState = GameRoundState(
            phase = GameState.DAY,
            dayNumber = 1,
            nightStep = null,
            nightActions = mutableListOf(),
            votes = mutableMapOf(),
            phaseRemainingSeconds = GameState.DAY.time,
        )

        return PhaseAdvanceResult(nextPhase = roundState.phase)
    }

    fun tick(): PhaseAdvanceResult? {
        if (roundState.phaseRemainingSeconds <= 1.seconds) {
            roundState = roundState.copy(phaseRemainingSeconds = 0.seconds)

            if (roundState.phase == GameState.NIGHT &&
                roundState.nightStep != null &&
                roundState.nightStep != NightStep.RESOLVE
            ) {
                advanceNightStepOnTimeout()
                return null
            }

            return advancePhase()
        }

        roundState = roundState.copy(
            phaseRemainingSeconds = roundState.phaseRemainingSeconds - 1.seconds,
        )

        println(phaseRemainingSeconds)
        println(roundState.phase)

        return null
    }

    fun advancePhase(): PhaseAdvanceResult {
        return when (roundState.phase) {
            GameState.MAYOR_VOTE -> advanceMayorVotePhase()
            GameState.DAY -> advanceDayPhase()
            GameState.VOTE -> advanceVotePhase()
            GameState.NIGHT -> advanceNightPhase()
        }
    }

    private fun advanceMayorVotePhase(): PhaseAdvanceResult {
        val standings = calculateMayorVoteStandings()
        val electedMayor = resolveMayorVote()
        roundState = roundState.copy(mayorPlayer = electedMayor)

        messenger.announceVotings(roundState.phase, standings, electedMayor)
        beginNightPhase()

        return PhaseAdvanceResult(
            nextPhase = roundState.phase,
            electedMayor = electedMayor,
            voteStandings = standings
        )
    }

    private fun advanceDayPhase(): PhaseAdvanceResult {
        if (isFirstDayWithoutMayor()) {
            beginMayorVoting()
        } else {
            beginVotePhase()
        }

        return PhaseAdvanceResult(nextPhase = roundState.phase)
    }

    private fun advanceVotePhase(): PhaseAdvanceResult {
        val standings = calculateVoteStandings()
        val votedOutPlayer = resolveVote()

        messenger.announceVotings(
            state = roundState.phase,
            standings = standings,
            eliminatedPlayers = listOfNotNull(votedOutPlayer)
        )

        val winner = checkWinCondition()
        if (winner != null) {
            return PhaseAdvanceResult(
                nextPhase = roundState.phase,
                winner = winner,
                eliminatedPlayers = listOfNotNull(votedOutPlayer),
                voteStandings = standings
            )
        }

        beginNightPhase()
        return PhaseAdvanceResult(
            nextPhase = roundState.phase,
            eliminatedPlayers = listOfNotNull(votedOutPlayer),
            voteStandings = standings
        )
    }

    private fun advanceNightPhase(): PhaseAdvanceResult {
        val nightResolution = resolveNight()
        val winner = checkWinCondition()

        if (winner != null) {
            return PhaseAdvanceResult(
                nextPhase = roundState.phase,
                winner = winner,
                eliminatedPlayers = nightResolution.eliminatedPlayers
            )
        }

        beginDayPhase(increaseDayNumber = true)
        return PhaseAdvanceResult(
            nextPhase = roundState.phase,
            eliminatedPlayers = nightResolution.eliminatedPlayers
        )
    }

    private fun isFirstDayWithoutMayor(): Boolean {
        return roundState.dayNumber == 1 && roundState.mayorPlayer == null
    }

    fun beginMayorVoting() {
        roundState = roundState.copy(
            phase = GameState.MAYOR_VOTE,
            phaseRemainingSeconds = GameState.MAYOR_VOTE.time,
            mayorVotes = mutableMapOf(),
            nightStep = null
        )

        messenger.announceMayorVotingStarted()
        service.setGameState(GameState.MAYOR_VOTE)
    }

    fun beginVotePhase() {
        roundState = roundState.copy(
            phase = GameState.VOTE,
            phaseRemainingSeconds = GameState.VOTE.time,
            votes = mutableMapOf(),
            nightStep = null
        )

        messenger.announceVillageVoteStarted()
        service.setGameState(GameState.VOTE)
    }

    fun beginNightPhase() {
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

    fun beginDayPhase(increaseDayNumber: Boolean = true) {
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

        service.setGameState(GameState.DAY)
    }

    fun submitMayorVote(voter: UUID, target: UUID): Boolean {
        return submitVote(
            expectedPhase = GameState.MAYOR_VOTE,
            votes = roundState.mayorVotes,
            voter = voter,
            target = target
        )
    }

    fun resolveMayorVote(): UUID? {
        if (roundState.phase != GameState.MAYOR_VOTE) return null
        return calculateMayorVoteStandings().firstOrNull()?.target
    }

    fun submitVote(voter: UUID, target: UUID): Boolean {
        return submitVote(
            expectedPhase = GameState.VOTE,
            votes = roundState.votes,
            voter = voter,
            target = target
        )
    }

    fun resolveVote(): UUID? {
        if (roundState.phase != GameState.VOTE) return null
        val killed = calculateVoteStandings().firstOrNull()?.target ?: return null
        service.executePlayer(killed)
        return killed
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

        votes[voter] = target
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

        return VoteResolver.calculateStandings(
            players = service.players,
            votes = roundState.votes
        ) { voterPlayer ->
            if (voterPlayer.role == WerwolfRoles.MAYOR) 2 else 1
        }
    }

    fun submitNightAction(action: NightAction): Boolean {
        if (roundState.phase != GameState.NIGHT) return false
        val actor = service.players[action.actor] ?: return false
        if (!actor.isAlive) return false
        if (!nightStepCoordinator().isActionAllowed(roundState.nightStep, action)) return false
        if (!nightResolver().isValid(action, actor.role)) return false

        replaceNightAction(action)
        advanceNightStepIfReady()

        return true
    }

    fun inspectWithSeer(actor: UUID, target: UUID): WerwolfRoles? {
        val action = NightAction.SeerInspect(actor = actor, target = target)
        if (!submitNightAction(action)) return null

        return SeerActions.inspectTarget(action, service.players)
    }

    fun peekWithGirl(actor: UUID): GirlPeekOutcome? {
        if (roundState.phase != GameState.NIGHT) return null
        if (roundState.nightStep != NightStep.GIRL) return null
        if (service.players[actor]?.role != WerwolfRoles.GIRL) return null
        if (service.players[actor]?.isAlive != true) return null

        val outcome = GirlActions.rollOutcome(service.players)
        val action = NightAction.GirlPeek(actor = actor, outcome = outcome)
        if (!submitNightAction(action)) return null

        return outcome
    }

    fun usePriestHolyWater(actor: UUID, target: UUID): PriestActionResult {
        if (roundState.phase != GameState.DAY) return PriestActionResult.WrongPhase

        val actorPlayer = service.players[actor] ?: return PriestActionResult.InvalidActor
        val targetPlayer = service.players[target] ?: return PriestActionResult.InvalidTarget

        if (actorPlayer.role != WerwolfRoles.PRIEST || !actorPlayer.isAlive) {
            return PriestActionResult.InvalidActor
        }

        if (!actorPlayer.hasPriestHolyWater) {
            return PriestActionResult.AlreadyUsed
        }

        if (!PriestActions.isValid(actorPlayer, targetPlayer)) {
            return PriestActionResult.InvalidTarget
        }

        actorPlayer.hasPriestHolyWater = false

        val resolution = PriestActions.resolve(actor, target, service.players)
        resolution.eliminatedPlayers.forEach(service::executePlayer)
        messenger.announcePriestHolyWater(actor, target, resolution.hitWerewolf)

        return PriestActionResult.Success(
            hitWerewolf = resolution.hitWerewolf,
            eliminatedPlayers = resolution.eliminatedPlayers,
            winner = checkWinCondition()
        )
    }

    fun resolveNight(): NightResolutionResult {
        if (roundState.phase != GameState.NIGHT) return NightResolutionResult()

        val resolution = nightResolver().resolve(roundState.nightActions)
        AmorActions.apply(service.players, resolution.lovers)
        WitchActions.apply(service.players, roundState.nightActions)
        messenger.announceLovers(resolution.lovers)
        resolution.eliminatedPlayers.forEach(service::executePlayer)

        roundState = roundState.copy(
            protectedPlayer = resolution.protectedPlayer,
            werewolfTarget = resolution.werewolfTarget,
            nightActions = mutableListOf()
        )

        return resolution
    }

    fun announceCurrentNightStep() {
        messenger.announceNightStep(roundState.nightStep)
    }

    fun canRoleActAtNight(role: WerwolfRoles): Boolean {
        if (roundState.phase != GameState.NIGHT) return false
        return roundState.nightStep?.activeRole == role
    }

    fun getWerewolfTargetFromLineOfSight(player: Player): UUID? {
        if (roundState.phase != GameState.NIGHT) return null
        if (service.getPlayerRole(player.uniqueId) != WerwolfRoles.WERWOLF) return null
        if (service.players[player.uniqueId]?.isAlive != true) return null

        val targetPlayer = player.getTargetEntity(50, true) as? Player ?: return null
        val targetId = targetPlayer.uniqueId

        if (service.players[targetId]?.isAlive != true) return null

        return targetId
    }

    fun getWitchHealTarget(player: Player): UUID? {
        if (roundState.phase != GameState.NIGHT) return null
        if (roundState.nightStep != NightStep.WITCH) return null
        if (service.getPlayerRole(player.uniqueId) != WerwolfRoles.WITCH) return null
        if (service.players[player.uniqueId]?.isAlive != true) return null

        val targetId = roundState.werewolfTarget ?: return null
        if (service.players[targetId]?.isAlive != true) return null

        return targetId
    }

    fun getDoctorHealTarget(player: Player): UUID? {
        if (roundState.phase != GameState.NIGHT) return null
        if (roundState.nightStep != NightStep.DOCTOR) return null
        if (service.getPlayerRole(player.uniqueId) != WerwolfRoles.DOCTOR) return null
        if (service.players[player.uniqueId]?.isAlive != true) return null

        val targetId = roundState.werewolfTarget ?: return null
        if (service.players[targetId]?.isAlive != true) return null

        return targetId
    }

    fun checkWinCondition(): GameOutcome? {
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

    private fun replaceNightAction(newAction: NightAction) {
        roundState.nightActions.removeAll { existingAction ->
            isSameNightActionSlot(existingAction, newAction)
        }
        roundState.nightActions.add(newAction)
    }

    private fun advanceNightStepIfReady() {
        if (roundState.nightStep == NightStep.WEREWOLVES) {
            roundState = roundState.copy(
                werewolfTarget = nightResolver().resolveWerewolfTarget(roundState.nightActions)
            )
        }

        val nextStep = nightStepCoordinator().nextStep(
            currentStep = roundState.nightStep,
            actions = roundState.nightActions
        ) ?: return

        setNightStep(nextStep)
    }

    private fun advanceNightStepOnTimeout() {
        if (roundState.nightStep == NightStep.WEREWOLVES) {
            roundState = roundState.copy(
                werewolfTarget = nightResolver().resolveWerewolfTarget(roundState.nightActions)
            )
        }

        val nextStep = nightStepCoordinator().nextStepAfterTimeout(roundState.nightStep)
            ?: NightStep.RESOLVE

        setNightStep(nextStep)
    }

    private fun setNightStep(step: NightStep, announce: Boolean = true) {
        roundState = roundState.copy(
            nightStep = step,
            phaseRemainingSeconds = step.time
        )

        if (announce) {
            messenger.announceNightStep(step)
        }
    }

    private fun isSameNightActionSlot(existingAction: NightAction, newAction: NightAction): Boolean {
        return existingAction.actor == newAction.actor &&
                existingAction::class == newAction::class
    }
}
