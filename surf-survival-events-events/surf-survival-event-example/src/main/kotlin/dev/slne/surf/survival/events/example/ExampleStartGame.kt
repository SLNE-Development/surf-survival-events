package dev.slne.surf.survival.events.example

import org.bukkit.Bukkit
import java.util.UUID

object ExampleStartGame {
    fun startExampleGame(playerList: ArrayDeque<UUID>) {

        /**
         * Don’t forget to put in the dependencies of the base: this compileOnly(project(":surf-survival-events-events:surf-survival-event-example"))
         *  This is where you would start your game logic, such as teleporting players, setting up the arena, etc.
         *  For this example, we'll just print the player UUIDs to the console.
         */
        println("Starting example game with players:")
        playerList.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            println(player)
        }

    }
}