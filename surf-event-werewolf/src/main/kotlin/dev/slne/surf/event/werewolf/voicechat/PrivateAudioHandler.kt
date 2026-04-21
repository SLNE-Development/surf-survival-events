package dev.slne.surf.event.werewolf.voicechat

import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
import org.bukkit.entity.Player
import java.util.*

class PrivateAudioHandler(private val channelId: UUID) {

    private val secretPlayers: MutableSet<UUID> = Collections.synchronizedSet(HashSet())
    private var voicechatChannel: AudioChannel? = null
    private var api: VoicechatServerApi? = null

    fun setSecretPlayers(players: List<Player>, voicechatApi: VoicechatServerApi?) {
        secretPlayers.clear()
        voicechatChannel = null

        voicechatApi?.let { api = it }

        if (players.isEmpty() || api == null) {
            return
        }

        val connection = api!!.getConnectionOf(players.first().uniqueId)
        if (connection != null) {
            voicechatChannel = api!!.createStaticAudioChannel(
                channelId,
                api!!.fromServerLevel(players.first().world),
                connection
            )
        }

        for (player in players) {
            secretPlayers.add(player.uniqueId)
        }
    }

    fun clearSecretPlayers() {
        secretPlayers.clear()
        voicechatChannel = null
    }

    fun isSecretPlayer(uuid: UUID): Boolean {
        return secretPlayers.contains(uuid)
    }

    fun onMicrophone(event: MicrophonePacketEvent) {
        val senderConnection = event.senderConnection ?: return
        val senderUuid = senderConnection.player.uuid

        if (!secretPlayers.contains(senderUuid)) return
        event.cancel()

        voicechatChannel?.let { channel ->
            try {
                channel.send(event.packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}