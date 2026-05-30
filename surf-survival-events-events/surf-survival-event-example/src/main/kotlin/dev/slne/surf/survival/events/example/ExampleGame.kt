package dev.slne.surf.survival.events.example

import dev.slne.surf.survival.events.base.game.GameHandler
import dev.slne.surf.survival.events.base.game.GameKey
import org.bukkit.Bukkit
import java.util.*

class ExampleGame : GameHandler {
    companion object {
        val KEY = GameKey.builder<ExampleGame>()
            .key("example")
            .displayName("EXAMPLE")
            .skullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkMWFiYTczZjYzOWY0YmM0MmJkNDgxOTZjNzE1MTk3YmUyNzEyYzNiOTYyYzk3ZWJmOWU5ZWQ4ZWZhMDI1In19fQ")
            .build()
    }


    override suspend fun beginGame(players: List<UUID>, spectators: Set<UUID>) {
        /**
         *  This is where you would start your game logic, such as teleporting players, setting up the arena, etc.
         *  For this example, we'll just print the player UUIDs to the console.
         */
        println("Starting example game with players:")
        players.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            println(player)
        }
    }
}