package dev.slne.surf.survival.events.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.survival.events.paper.menu.view.OverviewView
import dev.slne.surf.survival.events.paper.util.commands.registerCommands
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin()  {

    override suspend fun onLoadAsync() {
        OverviewView.register()
    }

    override suspend fun onEnableAsync() {
        registerCommands()

    }
}