package dev.slne.surf.survival.events.werewolf.util

import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class GameState(
    val displayName: Component,
    val time: Duration
) {
    NIGHT(
        buildText {
            darkBlue("Nacht")
        },
        time = 10.seconds
    ),

    DAY(
        buildText {
            gold("Tag")
        },
        time = 15.seconds
    ),

    VOTE(
        buildText {
            yellow("Abstimmung")
        },
        time = 20.seconds
    ),

    MAYOR_VOTE(
        buildText {
            yellow("Bürgermeisterwahl")
        },
        time = 20.seconds
    );
}
