package dev.slne.surf.survival.events.base.util.commands

import dev.slne.surf.survival.events.base.command.openMainMenu
import dev.slne.surf.survival.events.base.command.removeEventNpc
import dev.slne.surf.survival.events.base.command.spawnEventNpc

object CommandManager {
    fun registerCommands() {
        openMainMenu()
        removeEventNpc()
        spawnEventNpc()
    }
}

