package dev.slne.surf.event.werewolf.util

import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component

enum class GameState(
    val displayName: Component
) {
    NIGHT(
        buildText {
            darkBlue("Nacht")
        }
    ),

    DAY(
        buildText {
            gold("Tag")
        }
    ),

    VOTE(
        buildText {
            yellow("Abstimmung")
        }
    );
}
