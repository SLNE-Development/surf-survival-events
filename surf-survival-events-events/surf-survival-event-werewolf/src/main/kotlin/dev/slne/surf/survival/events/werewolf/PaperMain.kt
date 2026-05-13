package dev.slne.surf.survival.events.werewolf

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import de.maxhenkel.voicechat.api.BukkitVoicechatService
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.event.werewolf.commands.werewolfCommand
import dev.slne.surf.event.werewolf.listeners.WerewolfDisconnectListener
import dev.slne.surf.event.werewolf.service.WerewolfVisibilityCleanupListener
import dev.slne.surf.event.werewolf.voicechat.WerewolfVoicechatPlugin
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {

    override suspend fun onEnableAsync() {
        werewolfCommand()
        WerewolfDisconnectListener.register()
        WerewolfVisibilityCleanupListener.register()

        val voicechatService = server.servicesManager.load(BukkitVoicechatService::class.java)
        if (voicechatService != null) {
            val plugin = WerewolfVoicechatPlugin()
            voicechatService.registerPlugin(plugin)

            // Speichere die VoicechatServerApi für später Zugriff
//            voicechatService.registerPlugin(plugin)?.let {
//                WerewolfVoicechatPlugin.setVoicechatApi()
//            }
        }
    }

    override suspend fun onDisableAsync() {
    }
}

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

fun SurfComponentBuilder.inWerewolfColor(text: Any, vararg decoration: TextDecoration) =
    coloredComponent(text.toString(), TextColor.color(0, 120, 255), *decoration)
