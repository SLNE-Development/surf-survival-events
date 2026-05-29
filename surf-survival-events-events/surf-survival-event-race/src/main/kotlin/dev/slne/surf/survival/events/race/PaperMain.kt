package dev.slne.surf.survival.events.race

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.paper.event.register
import dev.slne.surf.survival.events.race.command.raceCommand
import dev.slne.surf.survival.events.race.config.SurfRaceConfig
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
    }
}
