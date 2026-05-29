package dev.slne.surf.survival.events.example

import dev.slne.surf.survival.events.base.game.GameHandler
import org.bukkit.Bukkit
import java.util.UUID

class ExampleGameHandler : GameHandler {
    override fun beginGame(players: ArrayDeque<UUID>, spectators: Set<UUID>) {
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