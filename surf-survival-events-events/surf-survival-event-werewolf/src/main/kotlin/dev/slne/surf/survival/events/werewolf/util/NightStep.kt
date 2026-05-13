package dev.slne.surf.event.werewolf.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class NightStep(
    val activeRole: WerwolfRoles?,
    val time: Duration,
) {
    AMOR(WerwolfRoles.AMOR, 60.seconds),
    WEREWOLVES(WerwolfRoles.WERWOLF, 60.seconds),
    GIRL(WerwolfRoles.GIRL, 60.seconds),
    SEER(WerwolfRoles.SEER, 60.seconds),
    DOCTOR(WerwolfRoles.DOCTOR, 60.seconds),
    WITCH(WerwolfRoles.WITCH, 60.seconds),
    SERIAL_KILLER(WerwolfRoles.SERIAL_KILLER, 60.seconds),
    RESOLVE(null, 1.seconds),
}
