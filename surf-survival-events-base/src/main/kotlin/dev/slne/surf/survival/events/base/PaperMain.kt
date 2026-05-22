package dev.slne.surf.survival.events.base

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.survival.events.base.listeners.JoinQuitListener
import dev.slne.surf.survival.events.base.menu.view.OverviewView
import dev.slne.surf.survival.events.base.listeners.NpcInteractListener
import dev.slne.surf.survival.events.base.service.NpcService
import dev.slne.surf.survival.events.base.util.commands.CommandManager
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        OverviewView.register()
    }

    override suspend fun onEnableAsync() {
        CommandManager.registerCommands()
        JoinQuitListener.register()
        NpcInteractListener.register()

        NpcService.showNpc()
    }
}