package dev.slne.surf.survival.events.werewolf.voicechat

import de.maxhenkel.voicechat.api.VoicechatApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WerewolfVoicechatPlugin : VoicechatPlugin {

    val PLUGIN_ID = "werewolf_voicechat"

    companion object {
        private val audioHandlers = ConcurrentHashMap<String, PrivateAudioHandler>()
        @Volatile
        private var voicechatApi: VoicechatServerApi? = null

        fun getAudioHandler(gameId: String): PrivateAudioHandler =
            audioHandlers.computeIfAbsent(gameId) { PrivateAudioHandler() }

        fun removeAudioHandler(gameId: String) {
            audioHandlers.remove(gameId)
        }

        fun getAllHandlers(): Map<String, PrivateAudioHandler> = audioHandlers.toMap()

        fun getVoicechatApi(): VoicechatServerApi? = voicechatApi

        fun setVoicechatApi(api: VoicechatServerApi?) {
            voicechatApi = api
        }
    }

    override fun getPluginId(): String = PLUGIN_ID

    override fun initialize(api: VoicechatApi) {
        setVoicechatApi(api as? VoicechatServerApi)
    }

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(VoicechatServerStartedEvent::class.java) { event ->
            setVoicechatApi(event.voicechat)
        }
        registration.registerEvent(VoicechatServerStoppedEvent::class.java) {
            setVoicechatApi(null)
        }
        registration.registerEvent(MicrophonePacketEvent::class.java) { event ->
            setVoicechatApi(event.voicechat)
            val senderUuid = event.senderConnection?.player?.uuid ?: return@registerEvent

            for (handler in getAllHandlers().values) {
                if (!handler.handlesPlayer(senderUuid)) {
                    continue
                }

                handler.onMicrophone(event)
                break
            }
        }
    }
}
