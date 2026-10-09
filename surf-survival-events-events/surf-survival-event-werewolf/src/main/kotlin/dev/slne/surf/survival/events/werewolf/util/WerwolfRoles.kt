package dev.slne.surf.survival.events.werewolf.util

import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component

enum class WerwolfRoles(
    val displayName: Component,
    val description: Component,
    val isHostile: Boolean = false
) {
    WERWOLF(
        buildText { red("Werwolf") },
        buildText { spacer("Jede Nacht kannst du mit den Wölfen darüber abstimmen, welcher Spieler getötet werden soll.") },
        isHostile = true
    ),

    VILLAGER(
        buildText { green("Dorfbewohner") },
        buildText { spacer("Du bist ein Dorfbewohner ohne spezielle Fähigkeiten.") },

    ),

    SEER(
        buildText { blue("Seherin") },
        buildText { spacer("Jede Nacht kannst du die Rolle eines anderen Spielers sehen.") },

    ),

    WITCH(
        buildText { darkPurple("Hexe") },
        buildText { spacer("Du hast zwei Tränke: Einer tötet und der andere heilt einen anderen Spieler. Der Heiltrank wird nur verbraucht, wenn der ausgewählte Spieler angegriffen wird. Du kannst in der ersten Nacht nicht töten.") },

    ),

    AMOR(
        buildText { lightPurple("Amor") },
        buildText { spacer("Während der ersten Nacht kannst du zwei Spieler wählen, welche das Pärchen bilden. Wenn einer der Geliebten stirbt, stirbt der andere Geliebte auch. Du gewinnst, wenn das Dorf gewinnt oder das Pärchen als letztes am Leben ist.") },

    ),

    DOCTOR(
        buildText { aqua("Doktor") },
        buildText { spacer("Du kannst jede Nacht einen Spieler beschützen. Dieser Spieler kann in dieser Nacht nicht getötet werden.") },

    ),

    GIRL(
        buildText { lightPurple("Mädchen") },
        buildText { spacer("Du darfst nachts heimlich die Augen öffnen und beobachten, wer die Werwölfe und ggf. Killer sind, riskierst dabei aber entdeckt zu werden.") },

    ),

    MAYOR(
        buildText { gold("Bürgermeister") },
        buildText { spacer("Du vertrittst das Dorf und daher zählt deine Stimme bei der Wahl doppelt.") },

    ),

    PRIEST(
        buildText { white("Priester") },
        buildText { spacer("Du kannst Weihwasser auf einen Spieler werfen. Wenn dieser ein Werwolf ist, stirbt er, andernfalls stirbst du.") },

    ),

    SERIAL_KILLER(
        buildText { darkRed("Serienmörder") },
        buildText { spacer("Jede Nacht kannst du einen Spieler töten.") },
        isHostile = true
    );
}