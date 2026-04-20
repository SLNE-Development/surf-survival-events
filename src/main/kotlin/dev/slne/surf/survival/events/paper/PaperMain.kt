package dev.slne.surf.survival.events.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.survival.events.paper.util.commands.registerCommands
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin()  {

    override suspend fun onEnableAsync() {
        registerCommands()

    }
}