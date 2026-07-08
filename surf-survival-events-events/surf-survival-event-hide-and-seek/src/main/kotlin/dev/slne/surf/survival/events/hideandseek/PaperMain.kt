package dev.slne.surf.survival.events.hideandseek

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.command.hideAndSeekCommand
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekGame
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        HideAndSeekConfig.init()
    }

    override suspend fun onEnableAsync() {
        hideAndSeekCommand()

        GameRegistry.register(HideAndSeekGame.KEY, HideAndSeekGame())
    }

    override suspend fun onDisableAsync() {
        if (GameService.isActiveGame(HideAndSeekGame.KEY)) {
            GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
        }

        GameRegistry.unregister(HideAndSeekGame.KEY)
    }
}
