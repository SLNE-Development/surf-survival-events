package dev.slne.surf.survival.events.red.light.green.light

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.red.light.green.light.command.rlglCommand
import dev.slne.surf.survival.events.red.light.green.light.config.RlglConfig
import dev.slne.surf.survival.events.red.light.green.light.game.RedLightGreenLightGame
import dev.slne.surf.survival.events.red.light.green.light.listener.AreaSelectionListener
import dev.slne.surf.survival.events.red.light.green.light.listener.RlglGameModeRestoreListener
import dev.slne.surf.survival.events.red.light.green.light.listener.RlglMovementListener
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        RlglConfig.init()
    }

    override suspend fun onEnableAsync() {
        rlglCommand()
        AreaSelectionListener.register()
        RlglMovementListener.register()
        RlglGameModeRestoreListener.register()

        GameRegistry.register(RedLightGreenLightGame.KEY, RedLightGreenLightGame())
    }

    override suspend fun onDisableAsync() {
        if (GameService.isActiveGame(RedLightGreenLightGame.KEY)) {
            GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
        }

        GameRegistry.unregister(RedLightGreenLightGame.KEY)
    }
}
