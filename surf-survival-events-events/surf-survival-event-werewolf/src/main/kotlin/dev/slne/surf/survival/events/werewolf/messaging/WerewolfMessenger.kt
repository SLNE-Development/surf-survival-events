package dev.slne.surf.survival.events.werewolf.messaging

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.*
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration
import java.util.*

class WerewolfMessenger(private val service: WerewolfService) {

    fun announceToPlayer(playerId: UUID, content: SurfComponentBuilder.() -> Unit) {
        playerId.toBukkitPlayer()?.sendText(content)
    }

    fun announceToAll(content: SurfComponentBuilder.() -> Unit) {
        val recipientIds = linkedSetOf<UUID>()
        recipientIds.addAll(service.players.keys)
        service.leader?.let(recipientIds::add)

        recipientIds.forEach { uuid ->
            uuid.toBukkitPlayer()?.sendText(content)
        }
    }

    fun announceToLeader(content: SurfComponentBuilder.() -> Unit) {
        service.leader?.toBukkitPlayer()?.sendText(content)
    }

    fun announceToRole(
        role: WerwolfRoles,
        onlyAlive: Boolean = true,
        content: SurfComponentBuilder.() -> Unit
    ) {
        service.players.values
            .filter { it.role == role }
            .filter { !onlyAlive || it.isAlive }
            .forEach { player ->
                player.uuid.toBukkitPlayer()?.sendText(content)
            }
    }

    fun announceToAlive(content: SurfComponentBuilder.() -> Unit) {
        val recipientIds = linkedSetOf<UUID>()
        service.getAlivePlayers().forEach { player ->
            recipientIds.add(player.uuid)
        }
        service.leader?.let(recipientIds::add)

        recipientIds.forEach { playerId ->
            playerId.toBukkitPlayer()?.sendText(content)
        }
    }

    fun announceLeaderRoles(roleMap: Map<UUID, WerwolfRoles>) {
        if (roleMap.isEmpty()) return

        announceToLeader {
            appendInfoPrefix()
            info("Rollen wurden verteilt:")

            roleMap.entries
                .sortedBy { playerName(it.key) }
                .forEach { (playerId, role) ->
                    appendNewInfoPrefixedLine()
                    variableValue(playerName(playerId))
                    appendSpace()
                    spacer("->")
                    appendSpace()
                    info(roleName(role))
                }
        }
    }

    fun announceLeaderVoteSubmitted(
        state: GameState,
        voterId: UUID,
        targetId: UUID,
        previousTargetId: UUID?,
    ) {
        announceToLeader {
            appendInfoPrefix()
            appendLeaderActionHeader(votePhaseName(state))
            appendActorTarget(voterId, targetId)
            appendPreviousTarget(previousTargetId)
        }
    }

    fun announceLeaderNightAction(
        action: NightAction,
        previousAction: NightAction?,
    ) {
        announceToLeader {
            appendInfoPrefix()

            when (action) {
                is NightAction.AmorLink -> {
                    appendLeaderActionHeader("Amor")
                    variableValue(playerName(action.actor))
                    appendSpace()
                    spacer("->")
                    appendSpace()
                    variableValue("${playerName(action.first)} + ${playerName(action.second)}")

                    val previousLink = previousAction as? NightAction.AmorLink
                    if (previousLink != null) {
                        appendSpace()
                        spacer("(")
                        info("zuvor")
                        appendSpace()
                        variableValue(playerName(previousLink.first))
                        appendSpace()
                        info("und")
                        appendSpace()
                        variableValue(playerName(previousLink.second))
                        spacer(")")
                    }
                }

                is NightAction.DoctorProtect -> {
                    appendLeaderActionHeader("Doktor")
                    appendActorTarget(action.actor, action.target)
                    val previousTarget = (previousAction as? NightAction.DoctorProtect)?.target
                    appendPreviousTarget(previousTarget)
                }

                is NightAction.GirlPeek,
                is NightAction.SeerInspect -> Unit

                is NightAction.SerialKillerKill -> {
                    appendLeaderActionHeader("Serienmoerder")
                    appendActorTarget(action.actor, action.target)
                    val previousTarget = (previousAction as? NightAction.SerialKillerKill)?.target
                    appendPreviousTarget(previousTarget)
                }

                is NightAction.WerewolfKill -> {
                    appendLeaderActionHeader("Werwolf")
                    appendActorTarget(action.actor, action.target)
                    val previousTarget = (previousAction as? NightAction.WerewolfKill)?.target
                    appendPreviousTarget(previousTarget)
                }

                is NightAction.WitchHeal -> {
                    appendLeaderActionHeader("Hexe Heiltrank")
                    appendActorTarget(action.actor, action.target)
                    val previousTarget = (previousAction as? NightAction.WitchHeal)?.target
                    appendPreviousTarget(previousTarget)
                }

                is NightAction.WitchPoison -> {
                    appendLeaderActionHeader("Hexe Gifttrank")
                    appendActorTarget(action.actor, action.target)
                    val previousTarget = (previousAction as? NightAction.WitchPoison)?.target
                    appendPreviousTarget(previousTarget)
                }
            }
        }
    }

    fun announceLeaderGirlPeek(actorId: UUID, outcome: GirlPeekOutcome) {
        announceToLeader {
            appendInfoPrefix()
            appendLeaderActionHeader("Maedchen")
            variableValue(playerName(actorId))
            appendSpace()
            spacer("->")
            appendSpace()

            when (outcome) {
                GirlPeekOutcome.CaughtByWerewolves -> error("erwischt")
                is GirlPeekOutcome.FoundWerewolf -> {
                    variableValue(playerName(outcome.target))
                }

                GirlPeekOutcome.TooDark -> info("nichts erkannt")
            }
        }
    }

    fun announceLeaderSeerInspection(
        actorId: UUID,
        targetId: UUID,
        inspectedRole: WerwolfRoles,
    ) {
        announceToLeader {
            appendInfoPrefix()
            appendLeaderActionHeader("Seherin")
            appendActorTarget(actorId, targetId)
            appendSpace()
            spacer("=")
            appendSpace()
            variableValue(roleName(inspectedRole))
        }
    }

    fun announceLeaderWerewolfTargetResolved(
        targetId: UUID?,
        votes: List<NightAction.WerewolfKill>,
    ) {
        val voteCounts = votes.groupingBy { it.target }.eachCount()
        val maxVotes = voteCounts.values.maxOrNull() ?: 0
        val hasMajority = maxVotes > votes.size / 2

        announceToLeader {
            appendInfoPrefix()

            if (votes.isEmpty()) {
                appendLeaderActionHeader("Werwolf-Ziel")
                info("keine Stimmen")
                return@announceToLeader
            }

            appendLeaderActionHeader("Werwolf-Ziel")

            if (targetId == null) {
                info("kein Ziel")
            } else {
                variableValue(playerName(targetId))
            }

            appendSpace()
            spacer("|")
            appendSpace()
            info("Stimmen:")
            appendSpace()
            variableValue(
                voteCounts.entries
                    .sortedByDescending { it.value }
                    .joinToString(", ") { (target, count) -> "${playerName(target)}=$count" }
            )

            if (!hasMajority) {
                appendSpace()
                spacer("|")
                appendSpace()
                info("kein Mehrheitsziel, Zufall aus Stimmen")
            }
        }
    }

    fun announceLeaderNightResolved(
        resolution: NightResolutionResult,
        doctorProtectedPlayer: UUID?,
        witchHealTarget: UUID?,
        witchPoisonTarget: UUID?,
        serialKillerTarget: UUID?,
        caughtGirls: List<UUID>,
    ) {
        announceToLeader {
            appendInfoPrefix()
            variableValue("Nachtauflösung")
            appendSpace()
            info("Leader-Übersicht")

            appendLeaderResolutionSection("Angriffe")
            appendLeaderResolutionPlayerLine("Werwölfe", resolution.werewolfTarget)
            appendLeaderResolutionPlayerLine("Serienmörder", serialKillerTarget)
            appendLeaderResolutionPlayerLine("Hexe Gifttrank", witchPoisonTarget)

            appendLeaderResolutionSection("Schutz")
            appendLeaderResolutionPlayerLine("Doktor", doctorProtectedPlayer)
            appendLeaderResolutionPlayerLine("Hexe Heiltrank", witchHealTarget)
            appendLeaderResolutionPlayerLine("Effektiv geschützt", resolution.protectedPlayer)

            appendLeaderResolutionSection("Sonderrollen")
            appendLeaderResolutionLoversLine(resolution.lovers)
            appendLeaderResolutionPlayerListLine("Mädchen erwischt", caughtGirls)

            appendLeaderResolutionSection("Ergebnis")
            appendLeaderResolutionPlayerListLine("Tote der Nacht", resolution.eliminatedPlayers, emptyMessage = "keine")
        }
    }

    fun announceLeaderNightStepTimeout(currentStep: NightStep, nextStep: NightStep) {
        announceToLeader {
            appendInfoPrefix()
            info("Nightstep")
            appendSpace()
            variableValue(nightStepName(currentStep))
            appendSpace()
            info("ist abgelaufen.")
            appendSpace()
            info("Nächster Step:")
            appendSpace()
            variableValue(nightStepName(nextStep))
        }
    }

    fun announceLeaderNightStepSkipped(currentStep: NightStep, nextStep: NightStep, skippedBy: UUID? = null) {
        announceToLeader {
            appendInfoPrefix()
            info("Nightstep")
            appendSpace()
            variableValue(nightStepName(currentStep))
            appendSpace()
            info("wurde übersprungen.")
            if (skippedBy != null) {
                appendSpace()
                info("Von:")
                appendSpace()
                variableValue(playerName(skippedBy))
            }
            appendSpace()
            info("Nächster Step:")
            appendSpace()
            variableValue(nightStepName(nextStep))
        }
    }

    fun announceLeaderEliminationChain(executedPlayers: List<UUID>, phase: GameState) {
        if (executedPlayers.isEmpty()) return

        announceToLeader {
            appendInfoPrefix()
            info(
                when (phase) {
                    GameState.NIGHT -> "Für den Tagesanbruch vorgemerkte Tote:"
                    GameState.DAY -> "Tagsüber ausgeschieden:"
                    GameState.VOTE -> "Durch Abstimmung ausgeschieden:"
                    GameState.MAYOR_VOTE -> "Ausgeschieden:"
                }
            )
            appendSpace()
            appendPlayerListOrNone(executedPlayers)
        }
    }

    fun announceLeaderVoiceChatOpened(playerIds: List<UUID>) {
        if (playerIds.isEmpty()) return

        announceToLeader {
            appendInfoPrefix()
            info("Privater Voice-Chat geöffnet für:")
            appendSpace()
            appendPlayerListOrNone(playerIds)
        }
    }

    fun announceLeaderVoiceChatClosed() {
        announceToLeader {
            appendInfoPrefix()
            info("Privater Voice-Chat wurde beendet.")
        }
    }

    fun announceMayorVotingStarted() {
        announceToAlive {
            appendInfoPrefix()
            info("Die Bürgermeisterwahl hat begonnen.")
            appendNewInfoPrefixedLine()
            info("Die Stimme des Bürgermeisters zählt doppelt so viel.")
            appendNewInfoPrefixedLine()
            append(createVoteCommandSuggestion())
        }
    }

    fun announceVillageVoteStarted() {
        announceToAlive {
            appendInfoPrefix()
            info("Das Dorf hat eine Abstimmung gestartet!")
            appendSpace()
            info("Wähle jemanden, der ein Werwolf sein könnte, oder enthalte dich!")
        }
    }

    fun announcePhaseStarted(state: GameState) {
        when (state) {
            GameState.NIGHT -> announceToAll {
                appendInfoPrefix()
                info("Die Nacht beginnt.")
            }

            GameState.DAY -> announceToAll {
                appendInfoPrefix()
                info("Der Tag beginnt.")
            }

            GameState.VOTE,
            GameState.MAYOR_VOTE -> Unit
        }
    }

    fun announceGameStarted() {
        announceToAll {
            appendSuccessPrefix()
            success("Das Spiel wurde gestartet!")
        }
    }

    fun announceGameStopped() {
        announceToAll {
            appendErrorPrefix()
            error("Das Spiel wurde gestoppt!")
        }
    }

    fun announceWinner(winner: GameOutcome) {
        announceToAll {
            appendSuccessPrefix()
            success("Das Spiel ist beendet. Gewinner: $winner")
        }
    }

    fun announceNightExecutionResults(executedPlayers: List<UUID>) {
        announceToAll {
            appendInfoPrefix()

            if (executedPlayers.isEmpty()) {
                info("In dieser Nacht ist niemand ausgeschieden.")
            } else {
                error(
                    if (executedPlayers.size == 1) {
                        "In der Nacht ausgeschieden:"
                    } else {
                        "In der Nacht ausgeschieden sind:"
                    }
                )
                appendSpace()
                variableValue(executedPlayers.joinToString(", ", transform = ::playerName))
            }
        }
    }

    fun announcePriestHolyWater(priestId: UUID, targetId: UUID, hitWerewolf: Boolean) {
        val priestName = playerName(priestId)
        val targetName = playerName(targetId)

        announceToAll {
            appendInfoPrefix()
            variableValue(priestName)
            appendSpace()
            info("hat Weihwasser auf")
            appendSpace()
            variableValue(targetName)
            success(".")
            appendSpace()

            if (hitWerewolf) {
                variableValue(targetName)
                appendSpace()
                error("war ein Werwolf und ist gestorben.")
            } else {
                variableValue(targetName)
                appendSpace()
                info("war kein Werwolf.")
                appendSpace()
                variableValue(priestName)
                appendSpace()
                error("ist gestorben.")
            }
        }
    }

    fun announceNightStep(step: NightStep?) {
        if (step != null) {
            announceToLeader {
                appendInfoPrefix()
                info("Nightstep:")
                appendSpace()
                variableValue(nightStepName(step))
            }
        }

        when (step) {
            NightStep.AMOR -> announceToRole(WerwolfRoles.AMOR) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf amor <spieler1> <spieler2>.")
            }

            NightStep.WEREWOLVES -> announceToRole(WerwolfRoles.WERWOLF) {
                appendInfoPrefix()
                info("Ihr seid jetzt am Zug. Nutzt /werewolf kill <spieler>.")
            }

            NightStep.GIRL -> announceToRole(WerwolfRoles.GIRL) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf girl, wenn du die Augen öffnen willst.")
            }

            NightStep.SEER -> announceToRole(WerwolfRoles.SEER) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf inspect <spieler>.")
            }

            NightStep.DOCTOR -> announceToRole(WerwolfRoles.DOCTOR) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf doctor <spieler>.")
                appendNewInfoPrefixedLine()
                info("Du kannst einen beliebigen lebenden Spieler heilen.")
            }

            NightStep.WITCH -> announceToRole(WerwolfRoles.WITCH) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf witch heal <spieler>, /werewolf witch kill <spieler> oder /werewolf witch skip.")
                appendNewInfoPrefixedLine()

                if (service.engine.currentWitchHealTargets.isNotEmpty()) {
                    appendNewInfoPrefixedLine()
                    info(
                        if (service.engine.currentWitchHealTargets.size == 1) {
                            "Ein Nachtopfer leuchtet für dich."
                        } else {
                            "Die Nachtopfer leuchten für dich."
                        }
                    )
                }

            }

            NightStep.SERIAL_KILLER -> announceToRole(WerwolfRoles.SERIAL_KILLER) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf serialkill <spieler>.")
            }

            NightStep.RESOLVE,
            null -> Unit
        }
    }

    fun announceLovers(lovers: Pair<UUID, UUID>?) {
        if (lovers == null) return

        val (firstId, secondId) = lovers
        val firstName = playerName(firstId)
        val secondName = playerName(secondId)

        announceToPlayer(firstId) {
            appendSuccessPrefix()
            success("Du bist nun ein Liebespaar mit")
            appendSpace()
            variableValue(secondName)
            appendSpace()
            success(".")
            appendNewInfoPrefixedLine()
            info("Wenn einer von euch stirbt, stirbt der andere auch.")
        }

        announceToPlayer(secondId) {
            appendSuccessPrefix()
            success("Du bist nun ein Liebespaar mit")
            appendSpace()
            variableValue(firstName)
            appendSpace()
            success(".")
            appendNewInfoPrefixedLine()
            info("Wenn einer von euch stirbt, stirbt der andere auch.")
        }
    }

    fun announceVotings(
        state: GameState,
        standings: List<VoteStanding>,
        chosenPlayer: UUID? = null,
        eliminatedPlayers: List<UUID> = emptyList(),
        content: SurfComponentBuilder.() -> Unit = {}
    ) {
        val type = when (state) {
            GameState.MAYOR_VOTE -> "Bürgermeisterwahl"
            GameState.VOTE -> "Dorfabstimmung"
            else -> "Abstimmung"
        }

        val podium = standings.take(3)

        announceToAlive {
            appendSuccessPrefix()
            success("Die Ergebnisse der")
            appendSpace()
            variableValue(type)
            appendSpace()
            success("sind da!")

            if (podium.isEmpty()) {
                appendNewInfoPrefixedLine()
                info("Es wurde keine gültige Stimme abgegeben.")
            } else {
                appendNewInfoPrefixedLine()
                info("Das Podium im Überblick:")

                podium.forEachIndexed { index, standing ->
                    appendNewInfoPrefixedLine()
                    variableValue("${index + 1}. Platz")
                    appendSpace()
                    spacer("-")
                    appendSpace()
                    variableValue(playerName(standing.target))
                    appendSpace()
                    spacer("(")
                    variableValue(standing.votes)
                    appendSpace()
                    info(if (standing.votes == 1) "Stimme" else "Stimmen")
                    spacer(")")
                }

                if (standings.size > podium.size) {
                    appendNewInfoPrefixedLine()
                    info("Weitere Kandidaten:")
                    appendSpace()
                    variableValue(standings.size - podium.size)
                }
            }

            when (state) {
                GameState.MAYOR_VOTE -> {
                    appendNewInfoPrefixedLine()
                    if (chosenPlayer != null) {
                        success("Neuer Bürgermeister:")
                        appendSpace()
                        variableValue(playerName(chosenPlayer))
                    } else {
                        info("Es konnte kein Bürgermeister bestimmt werden.")
                    }
                }

                GameState.VOTE -> {
                    appendNewInfoPrefixedLine()
                    val eliminatedPlayer = eliminatedPlayers.firstOrNull()
                    if (eliminatedPlayer != null) {
                        error("Aus dem Dorf ausgeschieden:")
                        appendSpace()
                        variableValue(playerName(eliminatedPlayer))
                    } else {
                        info("Niemand wurde aus dem Dorf entfernt.")
                    }
                }

                else -> Unit
            }

            content()
        }
    }

    private fun createVoteCommandSuggestion() = buildText {
        text("HIER", Colors.VARIABLE_VALUE, TextDecoration.UNDERLINED)
        hoverEvent(HoverEvent.showText(buildText { info("Klicke hier, um den Command in den Chat einzufügen!") }))
        clickEvent(ClickEvent.suggestCommand("/werewolf vote "))
    }

    private fun SurfComponentBuilder.appendPlayerListOrNone(playerIds: List<UUID>) {
        if (playerIds.isEmpty()) {
            info("niemand")
            return
        }

        variableValue(playerIds.joinToString(", ", transform = ::playerName))
    }

    private fun SurfComponentBuilder.appendPlayerOrNone(playerId: UUID?) {
        if (playerId == null) {
            info("niemand")
            return
        }

        variableValue(playerName(playerId))
    }

    private fun SurfComponentBuilder.appendLeaderResolutionSection(title: String) {
        appendNewInfoPrefixedLine()
        spacer("-----")
        appendSpace()
        variableValue(title)
        appendSpace()
        spacer("-----")
    }

    private fun SurfComponentBuilder.appendLeaderResolutionPlayerLine(label: String, playerId: UUID?) {
        appendNewInfoPrefixedLine()
        info(label)
        spacer(":")
        appendSpace()
        appendPlayerOrNone(playerId)
    }

    private fun SurfComponentBuilder.appendLeaderResolutionPlayerListLine(
        label: String,
        playerIds: List<UUID>,
        emptyMessage: String = "niemand",
    ) {
        appendNewInfoPrefixedLine()
        info(label)
        spacer(":")
        appendSpace()

        if (playerIds.isEmpty()) {
            info(emptyMessage)
        } else {
            variableValue(playerIds.joinToString(", ", transform = ::playerName))
        }
    }

    private fun SurfComponentBuilder.appendLeaderResolutionLoversLine(lovers: Pair<UUID, UUID>?) {
        appendNewInfoPrefixedLine()
        info("Liebespaar:")
        appendSpace()

        if (lovers == null) {
            info("keins")
            return
        }

        val (firstId, secondId) = lovers
        variableValue(playerName(firstId))
        appendSpace()
        info("und")
        appendSpace()
        variableValue(playerName(secondId))
    }

    private fun SurfComponentBuilder.appendLeaderActionHeader(actionName: String) {
        info("Aktion")
        appendSpace()
        spacer("|")
        appendSpace()
        variableValue(actionName)
        appendSpace()
        spacer("|")
        appendSpace()
    }

    private fun SurfComponentBuilder.appendActorTarget(actorId: UUID, targetId: UUID) {
        variableValue(playerName(actorId))
        appendSpace()
        spacer("->")
        appendSpace()
        variableValue(playerName(targetId))
    }

    private fun SurfComponentBuilder.appendPreviousTarget(previousTargetId: UUID?) {
        if (previousTargetId == null) return

        appendSpace()
        spacer("(")
        info("zuvor")
        appendSpace()
        variableValue(playerName(previousTargetId))
        spacer(")")
    }

    private fun votePhaseName(state: GameState): String = when (state) {
            GameState.MAYOR_VOTE -> "Bürgermeisterwahl"
            GameState.VOTE -> "Dorfabstimmung"
            GameState.DAY -> "Tag"
            GameState.NIGHT -> "Nacht"
        }

    private fun nightStepName(step: NightStep): String = when (step) {
            NightStep.AMOR -> "Amor"
            NightStep.WEREWOLVES -> "Werwölfe"
            NightStep.GIRL -> "Mädchen"
            NightStep.SEER -> "Seherin"
            NightStep.DOCTOR -> "Doktor"
            NightStep.WITCH -> "Hexe"
            NightStep.SERIAL_KILLER -> "Serienmörder"
            NightStep.RESOLVE -> "Auflösung"
        }

    private fun roleName(role: WerwolfRoles): String = when (role) {
            WerwolfRoles.WERWOLF -> "Werwolf"
            WerwolfRoles.VILLAGER -> "Dorfbewohner"
            WerwolfRoles.SEER -> "Seherin"
            WerwolfRoles.WITCH -> "Hexe"
            WerwolfRoles.AMOR -> "Amor"
            WerwolfRoles.DOCTOR -> "Doktor"
            WerwolfRoles.GIRL -> "Mädchen"
            WerwolfRoles.MAYOR -> "Bürgermeister"
            WerwolfRoles.PRIEST -> "Priester"
            WerwolfRoles.SERIAL_KILLER -> "Serienmörder"
        }

    private fun playerName(uuid: UUID): String =
        service.players[uuid]?.name ?: uuid.toBukkitPlayer()?.name ?: "Unbekannt"
}
