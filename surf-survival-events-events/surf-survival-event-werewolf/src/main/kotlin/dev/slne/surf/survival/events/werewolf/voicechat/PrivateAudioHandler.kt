package dev.slne.surf.survival.events.werewolf.voicechat

import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
import org.bukkit.entity.Player
import java.util.*

class PrivateAudioHandler {

    private val secretPlayers: MutableSet<UUID> = Collections.synchronizedSet(HashSet())
    private val silencedPlayers: MutableSet<UUID> = Collections.synchronizedSet(HashSet())
    private var api: VoicechatServerApi? = null

    fun configurePrivateChannel(
        secretPlayers: List<Player>,
        silencedPlayers: List<Player>,
        voicechatApi: VoicechatServerApi?,
    ) {
        this.secretPlayers.clear()
        this.silencedPlayers.clear()

        voicechatApi?.let { api = it }

        for (player in secretPlayers) {
            this.secretPlayers.add(player.uniqueId)
        }

        for (player in silencedPlayers) {
            this.silencedPlayers.add(player.uniqueId)
        }
    }

    fun clearPrivateChannel() {
        secretPlayers.clear()
        silencedPlayers.clear()
    }

    fun removePlayer(uuid: UUID) {
        secretPlayers.remove(uuid)
        silencedPlayers.remove(uuid)
    }

    fun handlesPlayer(uuid: UUID): Boolean {
        return secretPlayers.contains(uuid) || silencedPlayers.contains(uuid)
    }

    fun onMicrophone(event: MicrophonePacketEvent) {
        val senderConnection = event.senderConnection ?: return
        val senderUuid = senderConnection.player.uuid

        val voicechatApi = api ?: event.voicechat
        when {
            secretPlayers.contains(senderUuid) -> {
                event.cancel()
                val packet = event.packet.staticSoundPacketBuilder().build()

                secretPlayers
                    .asSequence()
                    .filter { it != senderUuid }
                    .mapNotNull(voicechatApi::getConnectionOf)
                    .forEach { connection ->
                        try {
                            voicechatApi.sendStaticSoundPacketTo(connection, packet)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            }

            silencedPlayers.contains(senderUuid) -> event.cancel()
        }
    }
}
