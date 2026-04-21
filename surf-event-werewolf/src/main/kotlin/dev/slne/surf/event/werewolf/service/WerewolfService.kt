package dev.slne.surf.event.werewolf.service

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.CommonComponents
import dev.slne.surf.api.core.messages.adventure.buildText
import dev.slne.surf.api.core.messages.adventure.playSound
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.adventure.showTitle
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.event.werewolf.dialog.WerewolfRoleViewDialoge
import dev.slne.surf.event.werewolf.messaging.WerewolfMessenger
import dev.slne.surf.event.werewolf.plugin
import dev.slne.surf.event.werewolf.scoreboard.addToWerewolfScoreboard
import dev.slne.surf.event.werewolf.scoreboard.removeFromWerewolfScoreboard
import dev.slne.surf.event.werewolf.util.*
import dev.slne.surf.event.werewolf.voicechat.WerewolfVoicechatPlugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import toBukkitPlayer
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed class WerewolfJoinResult {
    data object Success : WerewolfJoinResult()
    data object AlreadyInGame : WerewolfJoinResult()
    data object AlreadyStarted : WerewolfJoinResult()
    data class Error(val message: String) : WerewolfJoinResult()
}

sealed class WerewolfStartResult {
    data object Success : WerewolfStartResult()
    data object NotInLobbyPhase : WerewolfStartResult()
    data class NotEnoughPlayers(val current: Int, val required: Int) : WerewolfStartResult()
    data class Error(val message: String) : WerewolfStartResult()
}

class WerewolfService(val gameId: String) {

    internal val players = mutableMapOf<UUID, WerewolfPlayer>()

    val leader: UUID?
        get() = _leader

    private var _leader: UUID? = null

    val phase: GamePhase
        get() = _phase

    private var _phase = GamePhase.IDLE

    val state: GameState
        get() = _state

    private var _state = GameState.DAY

    private var werewolfTask: Job? = null

    val werewolfTime: Duration
        get() = _werewolfTime

    private var _werewolfTime: Duration = 0.seconds

    private val messenger = WerewolfMessenger(this)

    val aliveCount: Int
        get() = getAlivePlayers().size

    val totalCount: Int
        get() = players.size

    private val audioHandler = WerewolfVoicechatPlugin.getAudioHandler(gameId)

    fun openLobby(leaderUuid: UUID) {
        if (phase != GamePhase.IDLE) return

        players.clear()
        _phase = GamePhase.LOBBY
        _leader = leaderUuid
    }

    fun join(uuid: UUID): WerewolfJoinResult {
        if (phase != GamePhase.LOBBY) return WerewolfJoinResult.AlreadyStarted
        if (players.containsKey(uuid)) return WerewolfJoinResult.AlreadyInGame

        players[uuid] = WerewolfPlayer(uuid)

        uuid.toBukkitPlayer()?.addToWerewolfScoreboard() ?: return WerewolfJoinResult.Error("Dein Spieler konnte nicht gefunden werden!")

        announceToAll {
            appendSuccessPrefix()
            variableValue(uuid.toBukkitPlayer()?.name ?: "#Unbekannt")
            appendSpace()
            success("ist der Werwolf-Runde beigetreten!")
        }
        return WerewolfJoinResult.Success
    }

    fun start(): WerewolfStartResult {
        if (werewolfTask != null) error("Werewolf task is already running!")

        if (phase != GamePhase.LOBBY) {
            return WerewolfStartResult.NotInLobbyPhase
        }

        val minPlayers = 8
        if (players.size < minPlayers) {
            val result = WerewolfStartResult.NotEnoughPlayers(players.size, minPlayers)
            return result
        }

        try {
            _phase = GamePhase.RUNNING
            _state = GameState.DAY
            val roleMap = WerewolfRoleSelection.assignRoles(players.keys.toList())

            roleMap.forEach { (uuid, role) ->
                val werewolfPlayer = players[uuid] ?: return@forEach
                werewolfPlayer.role = role

                uuid.toBukkitPlayer()?.let {
                    it.sendText {
                        // ───────────── HEADER ─────────────
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))
                        appendSpace()
                        variableValue("DEINE ROLLE", TextDecoration.BOLD)
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── ROLE NAME ─────────────
                        info("Du bist: ")
                        append(role.displayName)

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── DESCRIPTION ─────────────
                        info("Rollenbeschreibung:")
                        appendNewInfoPrefixedLine()
                        appendSpace()
                        append(role.description)

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── FLAVOR LINE ─────────────
                        if (!role.isHostile) {
                            info("Spiele deine Rolle weise… das Dorf zählt auf dich.")
                        } else {
                            info("Du bist der Schrecken der Nacht… jage klug und bleibe unentdeckt.")
                        }

                        appendNewInfoPrefixedLine()
                        appendNewInfoPrefixedLine()

                        // ───────────── FOOTER ─────────────
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))
                        appendSpace()
                        append(role.displayName)
                        appendSpace()
                        spacer(CommonComponents.EM_DASH.content().repeat(18))
                    }

                    it.showDialog(
                        WerewolfRoleViewDialoge.create(it)
                    )
                }
            }

            werewolfTask = plugin.launch {
                while (isActive) {
                    allParticipants.forEach { participant ->
                        participant.sendActionBar(
                            buildText {
                                primary("Es ist ")
                                append(state.displayName)
                            }
                        )

                    }
                    if (state == GameState.NIGHT) {
                        getAlivePlayers().forEach {
                            it.uuid.toBukkitPlayer()?.addPotionEffect(
                                PotionEffect(
                                    PotionEffectType.DARKNESS,
                                    20,
                                    0,
                                    false,
                                    false,
                                    false
                                )
                            )
                        }
                    }

                    delay(1.seconds)
                    _werewolfTime += 1.seconds
                }
            }

            announceToAll {
                appendSuccessPrefix()
                success("Das Spiel wurde gestartet!")
            }
            players.forEach { (uuid, _) ->
                uuid.toBukkitPlayer()?.let {
                    it.showTitle {
                        title { primary("Werwolf gestartet") }
                        subtitle { variableValue("Viel Spaß!") }
                        times {
                            fadeIn(500.milliseconds)
                            stay(3.seconds)
                            fadeOut(500.milliseconds)
                        }
                    }
                    it.playSound(true) {
                        type(Sound.BLOCK_BEACON_ACTIVATE)
                        volume(.5f)
                        pitch(.5f)
                    }
                }
            }

            return WerewolfStartResult.Success
        } catch (e: Exception) {
            _phase = GamePhase.IDLE
            return WerewolfStartResult.Error(e.message ?: "Unbekannter Fehler beim Starten")
        }
    }

    fun stop() {
        werewolfTask?.cancel()
        werewolfTask = null
        _leader = null
        _phase = GamePhase.IDLE

        players.forEach { (uuid, _) ->
            uuid.toBukkitPlayer()?.removeFromWerewolfScoreboard()
        }

        announceToAll {
            appendErrorPrefix()
            error("Das Spiel wurde gestoppt!")
        }

        players.clear()

        // Cleanup Voice Chat
        audioHandler.clearSecretPlayers()
        WerewolfVoicechatPlugin.removeAudioHandler(gameId)
    }

    fun getAlivePlayers() = players.values.filter { it.isAlive }

    val allParticipants: List<Player>
        get() {
            val result = mutableListOf<Player>()

            players.keys.forEach { uuid ->
                uuid.toBukkitPlayer()?.let { result.add(it) }
            }

            leader?.toBukkitPlayer()?.let { if (!result.contains(it)) result.add(it) }
            return result
        }

    fun getAllPlayers() = players.values

    fun announceToAll(content: SurfComponentBuilder.() -> Unit) = messenger.announceToAll(content)

    fun announceToLeader(content: SurfComponentBuilder.() -> Unit) = messenger.announceToLeader(content)

    fun announceToRole(role: WerwolfRoles, onlyAlive: Boolean = true, content: SurfComponentBuilder.() -> Unit) = messenger.announceToRole(role, onlyAlive, content)

    fun announceToAlive(content: SurfComponentBuilder.() -> Unit) = messenger.announceToAlive(content)

    fun getPlayerRole(uuid: UUID): WerwolfRoles? {
        return players[uuid]?.role
    }

    fun movePlayersToPrivateChannel(playerList: List<Player>) {
        if (playerList.isEmpty()) return

        // Setze die Spieler im Audio-Handler mit der VoicechatServerApi
        val api = WerewolfVoicechatPlugin.getVoicechatApi()
        audioHandler.setSecretPlayers(playerList, api)

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendSuccessPrefix()
            success("Ihr könnt jetzt untereinander sprechen!")
        }
    }

    /**
     * Entfernt Spieler aus dem privaten Voice-Chat-Channel
     */
    fun removePlayersFromPrivateChannel() {
        audioHandler.clearSecretPlayers()

        announceToRole(WerwolfRoles.WERWOLF, onlyAlive = true) {
            appendErrorPrefix()
            error("Der private Voice-Chat wurde beendet.")
        }
    }
}
