package dev.slne.surf.event.werewolf.messaging

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.event.werewolf.service.WerewolfService
import dev.slne.surf.event.werewolf.util.*
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration
import java.util.*

class WerewolfMessenger(private val service: WerewolfService) {

    fun announceToPlayer(playerId: UUID, content: SurfComponentBuilder.() -> Unit) {
        playerId.toBukkitPlayer()?.sendText(content)
    }

    fun announceToAll(content: SurfComponentBuilder.() -> Unit) {
        service.players.keys.forEach { uuid ->
            uuid.toBukkitPlayer()?.sendText(content)
        }

        service.leader?.toBukkitPlayer()?.sendText(content)
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
        service.getAlivePlayers()
            .forEach { player ->
                player.uuid.toBukkitPlayer()?.sendText(content)
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
                info("Du kannst dich selbst oder das aktuelle Werwolf-Opfer heilen.")
                appendNewInfoPrefixedLine()
                info("Das Werwolf-Opfer leuchtet für dich.")
            }

            NightStep.WITCH -> announceToRole(WerwolfRoles.WITCH) {
                appendInfoPrefix()
                info("Du bist jetzt am Zug. Nutze /werewolf witch <heal|kill> <spieler>.")
                appendNewInfoPrefixedLine()
                info("Das Opfer der Werwölfe leuchtet für dich.")
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

    private fun playerName(uuid: UUID): String =
        service.players[uuid]?.name ?: uuid.toBukkitPlayer()?.name ?: "Unbekannt"
}
