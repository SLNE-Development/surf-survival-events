package dev.slne.surf.survival.events.werewolf.voicechat

import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
import org.bukkit.entity.Player
import java.util.*

class PrivateAudioHandler {

    private val lock = Any()
    private val secretPlayers: MutableSet<UUID> = HashSet()
    private val silencedPlayers: MutableSet<UUID> = HashSet()

    @Volatile
    private var api: VoicechatServerApi? = null

    fun configurePrivateChannel(
        secretPlayers: List<Player>,
        silencedPlayers: List<Player>,
        voicechatApi: VoicechatServerApi?,
    ): Unit = synchronized(lock) {
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

    fun clearPrivateChannel(): Unit = synchronized(lock) {
        secretPlayers.clear()
        silencedPlayers.clear()
    }

    fun removePlayer(uuid: UUID): Unit = synchronized(lock) {
        secretPlayers.remove(uuid)
        silencedPlayers.remove(uuid)
    }

    fun handlesPlayer(uuid: UUID): Boolean = synchronized(lock) {
        uuid in secretPlayers || uuid in silencedPlayers
    }

    fun onMicrophone(event: MicrophonePacketEvent) {
        val senderConnection = event.senderConnection ?: return
        val senderUuid = senderConnection.player.uuid

        val voicechatApi = api ?: event.voicechat

        val isSecret: Boolean
        val isSilenced: Boolean
        val secretRecipients: List<UUID>
        synchronized(lock) {
            isSecret = senderUuid in secretPlayers
            isSilenced = senderUuid in silencedPlayers
            secretRecipients = if (isSecret) secretPlayers.filter { it != senderUuid } else emptyList()
        }

        when {
            isSecret -> {
                event.cancel()
                val packet = event.packet.staticSoundPacketBuilder().build()

                secretRecipients
                    .mapNotNull(voicechatApi::getConnectionOf)
                    .forEach { connection ->
                        try {
                            voicechatApi.sendStaticSoundPacketTo(connection, packet)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            }

            isSilenced -> event.cancel()
        }
    }
}
