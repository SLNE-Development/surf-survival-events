package dev.slne.surf.survival.events.werewolf.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class NightStep(
    val activeRole: WerwolfRoles?,
    val time: Duration,
) {
    AMOR(WerwolfRoles.AMOR, 45.seconds),
    WEREWOLVES(WerwolfRoles.WERWOLF, 45.seconds),
    GIRL(WerwolfRoles.GIRL, 25.seconds),
    SEER(WerwolfRoles.SEER, 30.seconds),
    DOCTOR(WerwolfRoles.DOCTOR, 30.seconds),
    SERIAL_KILLER(WerwolfRoles.SERIAL_KILLER, 30.seconds),
    WITCH(WerwolfRoles.WITCH, 45.seconds),
    RESOLVE(null, 1.seconds),
}
