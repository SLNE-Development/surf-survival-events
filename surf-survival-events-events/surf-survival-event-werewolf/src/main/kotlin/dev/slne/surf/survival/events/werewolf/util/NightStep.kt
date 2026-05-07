package dev.slne.surf.event.werewolf.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class NightStep(
    val activeRole: WerwolfRoles?,
    val time: Duration,
) {
    AMOR(WerwolfRoles.AMOR, 20.seconds),
    WEREWOLVES(WerwolfRoles.WERWOLF, 25.seconds),
    GIRL(WerwolfRoles.GIRL, 12.seconds),
    SEER(WerwolfRoles.SEER, 15.seconds),
    DOCTOR(WerwolfRoles.DOCTOR, 15.seconds),
    WITCH(WerwolfRoles.WITCH, 20.seconds),
    SERIAL_KILLER(WerwolfRoles.SERIAL_KILLER, 15.seconds),
    RESOLVE(null, 1.seconds),
}
