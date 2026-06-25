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
        time = 30.seconds
    ),

    DAY(
        buildText {
            gold("Tag")
        },
        time = 120.seconds
    ),

    VOTE(
        buildText {
            yellow("Abstimmung")
        },
        time = 45.seconds
    ),

    MAYOR_VOTE(
        buildText {
            yellow("Bürgermeisterwahl")
        },
        time = 45.seconds
    );
}
