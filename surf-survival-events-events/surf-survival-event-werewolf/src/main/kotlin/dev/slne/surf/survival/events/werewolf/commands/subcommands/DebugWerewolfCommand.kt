package dev.slne.surf.survival.events.werewolf.commands.subcommands

import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import dev.slne.surf.survival.events.werewolf.util.GamePhase
import dev.slne.surf.survival.events.werewolf.util.GameState
import dev.slne.surf.survival.events.werewolf.util.NightStep
import dev.slne.surf.survival.events.werewolf.util.WerewolfCommandRequirements
import dev.slne.surf.survival.events.werewolf.util.WerewolfPlayer
import dev.slne.surf.survival.events.werewolf.util.WerwolfRoles
import org.bukkit.entity.Player

fun debugWerewolfCommand() = subcommand("debug") {
    withRequirement { sender -> WerewolfCommandRequirements.canUseLeaderDebug(sender) }
    subcommand(debugStatusWerewolfCommand())
    subcommand(debugRolesWerewolfCommand())
    subcommand(debugAdvancePhaseWerewolfCommand())
    subcommand(debugEliminateWerewolfCommand())
    subcommand(skipNightStepWerewolfCommand())
}

private fun debugStatusWerewolfCommand() = subcommand("status") {
    playerExecutor { player, _ ->
        val service = player.getLeaderDebugServiceOrNotify() ?: return@playerExecutor
        val alivePlayers = service.getAlivePlayers()
        val deadPlayers = service.getAllPlayers().filterNot(WerewolfPlayer::isAlive)
        val nightStep = service.engine.currentNightStep

        player.sendText {
            appendInfoPrefix()
            variableValue("Werwolf Debug Status")

            appendNewInfoPrefixedLine()
            info("Spiel:")
            appendSpace()
            variableValue(service.gameId)

            appendNewInfoPrefixedLine()
            info("Service-Phase:")
            appendSpace()
            variableValue(service.phase.name)

            appendNewInfoPrefixedLine()
            info("Game-State:")
            appendSpace()
            variableValue(gameStateName(service.engine.currentPhase))

            appendNewInfoPrefixedLine()
            info("Nightstep:")
            appendSpace()
            if (nightStep == null) {
                info("keiner")
            } else {
                variableValue(nightStepName(nightStep))
            }

            appendNewInfoPrefixedLine()
            info("Restzeit:")
            appendSpace()
            variableValue("${service.engine.phaseRemainingSeconds.inWholeSeconds}s")

            appendNewInfoPrefixedLine()
            info("Spielzeit:")
            appendSpace()
            variableValue("${service.werewolfTime.inWholeSeconds}s")

            appendNewInfoPrefixedLine()
            info("Spieler:")
            appendSpace()
            variableValue("${alivePlayers.size} lebend / ${deadPlayers.size} tot / ${service.totalCount} gesamt")

            appendNewInfoPrefixedLine()
            info("Phasenwechsel:")
            appendSpace()
            variableValue(if (service.isPhaseTransitioning) "läuft" else "nein")
        }
    }
}

private fun debugRolesWerewolfCommand() = subcommand("roles") {
    playerExecutor { player, _ ->
        val service = player.getLeaderDebugServiceOrNotify() ?: return@playerExecutor
        val players = service.getAllPlayers()
            .sortedWith(compareBy<WerewolfPlayer> { !it.isAlive }
                .thenBy { roleName(it.role) }
                .thenBy { it.name }
            )

        player.sendText {
            appendInfoPrefix()
            variableValue("Werwolf Rollenübersicht")

            if (players.isEmpty()) {
                appendNewInfoPrefixedLine()
                info("Keine Spieler im Spiel.")
                return@sendText
            }

            players.forEach { werewolfPlayer ->
                appendNewInfoPrefixedLine()
                variableValue(werewolfPlayer.name)
                appendSpace()
                spacer("|")
                appendSpace()
                info(roleName(werewolfPlayer.role))
                appendSpace()
                spacer("|")
                appendSpace()
                if (werewolfPlayer.isAlive) {
                    success("lebt")
                } else {
                    error("tot")
                }

                val extras = werewolfPlayer.debugExtras(service)
                if (extras.isNotEmpty()) {
                    appendSpace()
                    spacer("|")
                    appendSpace()
                    info(extras.joinToString(", "))
                }
            }
        }
    }
}

private fun debugAdvancePhaseWerewolfCommand() = subcommand("advancePhase") {
    playerExecutor { player, _ ->
        val service = player.getLeaderDebugServiceOrNotify() ?: return@playerExecutor

        if (service.phase != GamePhase.RUNNING) {
            player.sendText {
                appendErrorPrefix()
                error("Das Spiel läuft gerade nicht.")
            }
            return@playerExecutor
        }

        if (service.isPhaseTransitioning) {
            player.sendText {
                appendErrorPrefix()
                error("Der Phasenwechsel läuft gerade noch.")
            }
            return@playerExecutor
        }

        val result = service.debugAdvancePhase()
        if (result == null) {
            player.sendText {
                appendErrorPrefix()
                error("Die Phase konnte nicht vorgespult werden.")
            }
            return@playerExecutor
        }

        player.sendText {
            appendSuccessPrefix()
            success("Phase wurde vorgespult. Neue Phase:")
            appendSpace()
            variableValue(gameStateName(result.nextPhase))
        }
    }
}

private fun debugEliminateWerewolfCommand() = subcommand("eliminate") {
    withArguments(EntitySelectorArgument.OnePlayer("targetPlayer"))

    playerExecutor { player, arguments ->
        val targetPlayer: Player by arguments
        val service = player.getLeaderDebugServiceOrNotify() ?: return@playerExecutor
        val target = service.getAllPlayers().firstOrNull { it.uuid == targetPlayer.uniqueId }

        if (service.phase != GamePhase.RUNNING) {
            player.sendText {
                appendErrorPrefix()
                error("Das Spiel läuft gerade nicht.")
            }
            return@playerExecutor
        }

        if (target == null) {
            player.sendText {
                appendErrorPrefix()
                error("Dieser Spieler ist kein Teilnehmer des Werwolf-Spiels.")
            }
            return@playerExecutor
        }

        if (!target.isAlive) {
            player.sendText {
                appendErrorPrefix()
                error("Dieser Spieler ist bereits tot.")
            }
            return@playerExecutor
        }

        service.executePlayer(target.uuid)
        service.engine.checkWinCondition()?.let(service::finishGame)

        player.sendText {
            appendSuccessPrefix()
            success("Debug-Eliminierung ausgeführt für")
            appendSpace()
            variableValue(target.name)
            if (service.state == GameState.NIGHT) {
                appendSpace()
                info("(für den Tagesanbruch vorgemerkt)")
            }
        }
    }
}

private fun Player.getLeaderDebugServiceOrNotify(): WerewolfService? {
    val service = WerewolfGameManager.getGameForPlayer(uniqueId)

    if (service == null) {
        sendText {
            appendErrorPrefix()
            error("Du leitest aktuell kein Werwolf-Spiel.")
        }
        return null
    }

    if (service.leader != uniqueId) {
        sendText {
            appendErrorPrefix()
            error("Nur der Erzähler darf Debug-Commands benutzen.")
        }
        return null
    }

    return service
}

private fun WerewolfPlayer.debugExtras(service: WerewolfService): List<String> = buildList {
    inLoveWith?.let { loverId ->
        val loverName = service.getAllPlayers().firstOrNull { it.uuid == loverId }?.name ?: "Unbekannt"
        add("verliebt mit $loverName")
    }

    if (role == WerwolfRoles.WITCH) {
        add("Heiltrank: ${if (hasWitchHealPotion) "ja" else "nein"}")
        add("Gifttrank: ${if (hasWitchPoisonPotion) "ja" else "nein"}")
    }

    if (role == WerwolfRoles.PRIEST) {
        add("Weihwasser: ${if (hasPriestHolyWater) "ja" else "nein"}")
    }
}

private fun gameStateName(state: GameState): String = when (state) {
    GameState.MAYOR_VOTE -> "Bürgermeisterwahl"
    GameState.DAY -> "Tag"
    GameState.VOTE -> "Dorfabstimmung"
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
