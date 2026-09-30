package dev.slne.surf.survival.events.red.light.green.light.service

import java.util.UUID

data class RoundStanding(
    val uuid: UUID,
    val name: String,
    val placement: Int?,
    val finishTimeMillis: Long?
) {
    val finished: Boolean get() = placement != null
}

sealed interface OpenRoundResult {
    data class Opened(val id: String) : OpenRoundResult
    data object AlreadyOpen : OpenRoundResult
    data object RoundInProgress : OpenRoundResult
}

enum class JoinRoundResult {
    NO_ROUND_OPEN,
    WRONG_ID,
    ALREADY_JOINED,
    JOINED
}

enum class LeaveRoundResult {
    NO_ROUND_OPEN,
    WRONG_ID,
    NOT_IN_ROUND,
    LEFT
}

data class ConfigCheck(val label: String, val configured: Boolean)

enum class StartRoundResult {
    NO_ROUND_OPEN,
    WRONG_ID,
    NO_PLAYERS_JOINED,
    ALREADY_RUNNING,
    STARTED
}

enum class RemovePlayerResult {
    NO_ROUND_OPEN,
    WRONG_ID,
    NOT_IN_ROUND,
    REMOVED
}