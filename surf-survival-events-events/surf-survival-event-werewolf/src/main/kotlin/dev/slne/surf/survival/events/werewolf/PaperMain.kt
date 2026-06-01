package dev.slne.surf.survival.events.werewolf

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import de.maxhenkel.voicechat.api.BukkitVoicechatService
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.werewolf.commands.werewolfCommand
import dev.slne.surf.survival.events.werewolf.game.WerewolfGame
import dev.slne.surf.survival.events.werewolf.service.WerewolfVisibilityCleanupListener
import dev.slne.surf.survival.events.werewolf.voicechat.WerewolfVoicechatPlugin
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {

    override suspend fun onEnableAsync() {
        werewolfCommand()
        WerewolfVisibilityCleanupListener.register()
        GameRegistry.register(WerewolfGame.KEY, WerewolfGame())

        val voicechatService = server.servicesManager.load(BukkitVoicechatService::class.java)
        if (voicechatService != null) {
            val plugin = WerewolfVoicechatPlugin()
            voicechatService.registerPlugin(plugin)
        }
    }

    override suspend fun onDisableAsync() {
        if (GameService.isActiveGame(WerewolfGame.KEY)) {
            GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
        }

        GameRegistry.unregister(WerewolfGame.KEY)
    }
}

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

fun SurfComponentBuilder.inWerewolfColor(text: Any, vararg decoration: TextDecoration) =
    coloredComponent(text.toString(), TextColor.color(0, 120, 255), *decoration)
