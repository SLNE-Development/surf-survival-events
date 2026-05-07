package dev.slne.surf.event.werewolf.voicechat

import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
import java.util.*

class WerewolfVoicechatPlugin : VoicechatPlugin {

    companion object {
        private val audioHandlers = mutableMapOf<String, PrivateAudioHandler>()
        private var voicechatApi: VoicechatServerApi? = null

        fun getAudioHandler(gameId: String): PrivateAudioHandler {
            return audioHandlers.getOrPut(gameId) {
                PrivateAudioHandler(UUID.randomUUID())
            }
        }

        fun removeAudioHandler(gameId: String) {
            audioHandlers.remove(gameId)
        }

        fun getAllHandlers(): Map<String, PrivateAudioHandler> = audioHandlers.toMap()
        
        fun getVoicechatApi(): VoicechatServerApi? = voicechatApi
        
        fun setVoicechatApi(api: VoicechatServerApi) {
            voicechatApi = api
        }
    }

    override fun getPluginId(): String {
        return "werewolf_voice"
    }

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(MicrophonePacketEvent::class.java) { event ->
            getAllHandlers().values.forEach { handler ->
                val senderUuid = event.senderConnection?.player?.uuid
                if (senderUuid != null && handler.isSecretPlayer(senderUuid)) {
                    handler.onMicrophone(event)
                }
            }
        }
    }
}

