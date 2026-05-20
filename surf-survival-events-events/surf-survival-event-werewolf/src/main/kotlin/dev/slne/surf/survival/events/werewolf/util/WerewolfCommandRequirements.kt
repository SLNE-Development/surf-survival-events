package dev.slne.surf.survival.events.werewolf.util

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.CommandAPI
import dev.slne.surf.survival.events.werewolf.plugin
import dev.slne.surf.survival.events.werewolf.service.WerewolfGameManager
import dev.slne.surf.survival.events.werewolf.service.WerewolfService
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object WerewolfCommandRequirements {

    fun update(player: Player?) {
        if (player == null) return
        update(listOf(player))
    }

    fun update(players: Iterable<Player>) {
        players.distinctBy { it.uniqueId }.forEach { player ->
            if (!player.isOnline) return@forEach
            plugin.launch {
                if (!player.isOnline) return@launch
                withContext(plugin.entityDispatcher(player)) {
                    CommandAPI.updateRequirements(player)
                }
            }
        }
    }

    fun canJoinGame(sender: CommandSender): Boolean {
        val player = sender as? Player ?: return false
        return WerewolfGameManager.getGameForPlayer(player.uniqueId) == null
    }

    fun canStartGame(sender: CommandSender): Boolean = withGame(sender) { player, service ->
        service.leader == player.uniqueId && service.phase == GamePhase.LOBBY
    }

    fun canStopGame(sender: CommandSender): Boolean = withGame(sender) { player, service ->
        service.leader == player.uniqueId && service.phase != GamePhase.IDLE
    }

    fun canVote(sender: CommandSender): Boolean = withAliveParticipant(sender) { _, service, _ ->
        val currentPhase = service.engine.currentPhase
        currentPhase == GameState.MAYOR_VOTE || currentPhase == GameState.VOTE
    }

    fun canActAsWerewolf(sender: CommandSender): Boolean =
        canUseNightRole(sender, WerwolfRoles.WERWOLF, NightStep.WEREWOLVES)

    fun canActAsAmor(sender: CommandSender): Boolean =
        canUseNightRole(sender, WerwolfRoles.AMOR, NightStep.AMOR)

    fun canActAsGirl(sender: CommandSender): Boolean =
        canUseNightRole(sender, WerwolfRoles.GIRL, NightStep.GIRL)

    fun canActAsSeer(sender: CommandSender): Boolean =
        canUseNightRole(sender, WerwolfRoles.SEER, NightStep.SEER)

    fun canActAsDoctor(sender: CommandSender): Boolean =
        canUseNightRole(sender, WerwolfRoles.DOCTOR, NightStep.DOCTOR)

    fun canActAsSerialKiller(sender: CommandSender): Boolean =
        canUseNightRole(sender, WerwolfRoles.SERIAL_KILLER, NightStep.SERIAL_KILLER)

    fun canActAsWitch(sender: CommandSender): Boolean =
        withAliveParticipant(sender) { _, service, participant ->
            service.engine.currentPhase == GameState.NIGHT &&
                service.engine.currentNightStep == NightStep.WITCH &&
                participant.role == WerwolfRoles.WITCH &&
                (participant.hasWitchHealPotion || participant.hasWitchPoisonPotion)
        }

    fun canActAsPriest(sender: CommandSender): Boolean =
        withAliveParticipant(sender) { _, service, participant ->
            service.engine.currentPhase == GameState.DAY &&
                participant.role == WerwolfRoles.PRIEST &&
                participant.hasPriestHolyWater
        }

    fun canDebugSkipNightStep(sender: CommandSender): Boolean = withGame(sender) { player, service ->
        service.leader == player.uniqueId &&
            !service.isPhaseTransitioning &&
            service.engine.currentPhase == GameState.NIGHT &&
            service.engine.currentNightStep != null &&
            service.engine.currentNightStep != NightStep.RESOLVE
    }

    private fun canUseNightRole(
        sender: CommandSender,
        role: WerwolfRoles,
        nightStep: NightStep,
    ): Boolean = withAliveParticipant(sender) { _, service, participant ->
        service.engine.currentPhase == GameState.NIGHT &&
            service.engine.currentNightStep == nightStep &&
            participant.role == role
    }

    private inline fun withGame(
        sender: CommandSender,
        predicate: (Player, WerewolfService) -> Boolean,
    ): Boolean {
        val player = sender as? Player ?: return false
        val service = WerewolfGameManager.getGameForPlayer(player.uniqueId) ?: return false
        return predicate(player, service)
    }

    private inline fun withAliveParticipant(
        sender: CommandSender,
        predicate: (Player, WerewolfService, WerewolfPlayer) -> Boolean,
    ): Boolean = withGame(sender) { player, service ->
        val participant = service.players[player.uniqueId] ?: return@withGame false
        if (!participant.isAlive || service.isPhaseTransitioning) return@withGame false
        predicate(player, service, participant)
    }
}
