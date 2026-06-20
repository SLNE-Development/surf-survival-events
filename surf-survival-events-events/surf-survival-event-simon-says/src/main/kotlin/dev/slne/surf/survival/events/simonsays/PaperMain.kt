package dev.slne.surf.survival.events.simonsays

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.survival.events.base.game.GameRegistry
import org.bukkit.plugin.java.JavaPlugin

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onEnableAsync() {
        GameRegistry.register(SimonSaysGame.KEY, SimonSaysGame())
    }
}

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)