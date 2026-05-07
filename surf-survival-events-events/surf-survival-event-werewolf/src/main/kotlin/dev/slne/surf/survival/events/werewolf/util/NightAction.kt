package dev.slne.surf.event.werewolf.util

import java.util.*

sealed class NightAction {
    abstract val actor: UUID

    data class WerewolfKill(override val actor: UUID, val target: UUID) : NightAction()
    data class SeerInspect(override val actor: UUID, val target: UUID) : NightAction()
    data class DoctorProtect(override val actor: UUID, val target: UUID) : NightAction()
    data class WitchHeal(override val actor: UUID, val target: UUID) : NightAction()
    data class WitchPoison(override val actor: UUID, val target: UUID) : NightAction()
    data class AmorLink(override val actor: UUID, val first: UUID, val second: UUID) : NightAction()
    data class GirlPeek(override val actor: UUID, val outcome: GirlPeekOutcome) : NightAction()
    data class SerialKillerKill(override val actor: UUID, val target: UUID) : NightAction()
}
