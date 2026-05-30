package dev.slne.surf.survival.events.example

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.survival.events.base.game.GameRegistry
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        plugin.logger.info("Loading surf-survival-event-example plugin...")
        plugin.logger.warning("This plugin should not be used in production!")
    }

    override suspend fun onEnableAsync() {
        plugin.logger.info("Enabling surf-survival-event-example plugin...")

        GameRegistry.register(ExampleGame.KEY, ExampleGame())
    }

    override suspend fun onDisableAsync() {
        plugin.logger.info("Disabling surf-survival-event-example plugin...")

        GameRegistry.unregister(ExampleGame.KEY)
    }
}