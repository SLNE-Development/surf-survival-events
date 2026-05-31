package dev.slne.surf.survival.events.race

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.game.GameStopReason
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.race.command.raceCommand
import dev.slne.surf.survival.events.race.config.RaceConfig
import dev.slne.surf.survival.events.race.game.RaceGame
import dev.slne.surf.survival.events.race.listener.RaceListener
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        RaceConfig.init()
    }

    override suspend fun onEnableAsync() {
        raceCommand()
        RaceListener.register()

        GameRegistry.register(RaceGame.KEY, RaceGame())
    }

    override suspend fun onDisableAsync() {
        if (GameService.isActiveGame(RaceGame.KEY)) {
            GameService.stopGameAndWait(GameStopReason.PLUGIN_DISABLE)
        }

        GameRegistry.unregister(RaceGame.KEY)
    }
}
