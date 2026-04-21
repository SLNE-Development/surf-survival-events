package dev.slne.surf.event.werewolf.util

import dev.slne.surf.api.core.messages.adventure.buildText
import net.kyori.adventure.text.Component

enum class WerwolfRoles(
    val displayName: Component,
    val description: Component,
    val isHostile: Boolean = false
) {
    WERWOLF(
        buildText { red("Werwolf") },
        buildText { spacer("Du verspeist jede Nacht einen Dorfbewohner.") },
        isHostile = true
    ),

    VILLAGER(
        buildText { green("Dorfbewohner") },
        buildText { spacer("Ein einfacher Bürger ohne Spezialkräfte.") },
        isHostile = false
    ),

    SEER(
        buildText { blue("Seherin") },
        buildText { spacer("Erkenne jede Nacht die Rolle eines Spielers.") },
        isHostile = false
    ),

    WITCH(
        buildText { darkPurple("Hexe") },
        buildText { spacer("Besitzt einen Heil- und einen Gifttrank.") },
        isHostile = false
    ),

    AMOR(
        buildText { lightPurple("Amor") },
        buildText { spacer("Verbinde zwei Herzen zu einem Liebespaar.") },
        isHostile = false
    ),

    DOCTOR(
        buildText { aqua("Doktor") },
        buildText { spacer("Schütze jede Nacht jemanden vor den Wölfen.") },
        isHostile = false
    ),

    GIRL(
        buildText { lightPurple("Mädchen") },
        buildText { spacer("Du darfst nachts heimlich blinzeln.") },
        isHostile = false
    ),

    MAYOR(
        buildText { gold("Bürgermeister") },
        buildText { spacer("Deine Stimme zählt bei der Wahl doppelt.") },
        isHostile = false
    ),

    PRIEST(
        buildText { white("Priester") },
        buildText { spacer("Du bespritzt jemanden mit Weihwasser.") },
        isHostile = false
    ),

    SERIAL_KILLER(
        buildText { darkRed("Serienmörder") },
        buildText { spacer("Du spielst allein und tötest jeden.") },
        isHostile = true
    );
}
