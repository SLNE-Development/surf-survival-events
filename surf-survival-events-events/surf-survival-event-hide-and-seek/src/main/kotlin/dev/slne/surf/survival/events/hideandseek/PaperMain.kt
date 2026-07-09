package dev.slne.surf.survival.events.hideandseek

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.game.GameWorldService
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.command.hideAndSeekCommand
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekGame
import kotlinx.coroutines.CoroutineStart
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onEnableAsync() {
        hideAndSeekCommand()

        val game = HideAndSeekGame()
        GameRegistry.register(HideAndSeekGame.KEY, game)

        preloadEventWorldAndConfig(game)
    }

    private fun preloadEventWorldAndConfig(game: HideAndSeekGame) {
        launch(globalRegionDispatcher, CoroutineStart.DEFAULT) {
            try {
                GameWorldService.loadOrCreateEventWorld(
                    HideAndSeekGame.KEY,
                    game::customizeWorldCreator,
                    game::customizeEventWorld
                )
            } catch (throwable: Throwable) {
                componentLogger.error("Failed to preload the hide and seek event world", throwable)
            }

            HideAndSeekConfig.init()
        }
    }

    override suspend fun onDisableAsync() {
        if (GameService.isActiveGame(HideAndSeekGame.KEY)) {
            GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
        }

        GameRegistry.unregister(HideAndSeekGame.KEY)
    }
}
