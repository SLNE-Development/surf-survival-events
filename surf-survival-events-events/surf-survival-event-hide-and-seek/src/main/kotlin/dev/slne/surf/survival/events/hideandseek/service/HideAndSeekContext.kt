package dev.slne.surf.survival.events.hideandseek.service

import dev.slne.surf.survival.events.base.game.GameContext
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekGame

internal fun currentContext(): GameContext? =
    GameService.snapshot()?.takeIf { it.key == HideAndSeekGame.KEY }
