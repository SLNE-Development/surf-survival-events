package dev.slne.surf.survival.events.base

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.api.paper.inventory.framework.register
import dev.slne.surf.survival.events.base.command.CommandManager
import dev.slne.surf.survival.events.base.config.SurvivalEventsConfig
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.listeners.JoinQuitListener
import dev.slne.surf.survival.events.base.menu.view.eventGamesOverviewView
import dev.slne.surf.survival.events.base.service.GameService
import org.bukkit.plugin.java.JavaPlugin

internal val plugin: PaperMain get() = JavaPlugin.getPlugin(PaperMain::class.java)

internal class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        eventGamesOverviewView.register()
    }

    override suspend fun onEnableAsync() {
        SurvivalEventsConfig.init()

        CommandManager.registerCommands()
        JoinQuitListener.register()
        GameService.syncOnlinePlayers()
    }

    override suspend fun onDisableAsync() {
        GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
    }
}