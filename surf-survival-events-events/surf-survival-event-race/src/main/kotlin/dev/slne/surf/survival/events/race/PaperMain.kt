package dev.slne.surf.survival.events.race

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.survival.events.base.game.GameRegistry
import dev.slne.surf.survival.events.base.util.Games
import dev.slne.surf.survival.events.race.command.raceCommand
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
import dev.slne.surf.survival.events.race.game.RaceGameHandler
import dev.slne.surf.survival.events.race.listener.QuitListener
import dev.slne.surf.survival.events.race.listener.RaceListener
import org.bukkit.plugin.java.JavaPlugin

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        SurfRaceConfig.init()
    }

    override suspend fun onEnableAsync() {
        raceCommand()
        RaceListener.register()
        QuitListener.register()

        GameRegistry.register(Games.RACE, RaceGameHandler())
    }

    override suspend fun onDisableAsync() {
        GameRegistry.unregister(Games.RACE)
    }
}
