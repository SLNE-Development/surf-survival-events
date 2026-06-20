package dev.slne.surf.survival.events.simonsays

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.api.core.util.runAtFixedRate
import dev.slne.surf.api.paper.util.forEachPlayerInRegion
import dev.slne.surf.survival.events.base.game.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import org.bukkit.*
import org.bukkit.potion.PotionEffectType
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class SimonSaysGame : GameHandler {
    companion object {
        val KEY = GameKey.of<SimonSaysGame>(
            "Simon Says",
            "simon-says",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODk3YmY3N2ExY2I1MThiM2I0OTRmOWRiNTBiMzhhNmRmNWU4MjZmZDhiNDg1NzkyYjRlNWI3NGRhYWIzZTRjIn19fQ=="
        )
    }

    private var effectJob: Job? = null

    override val options: GameOptions = GameOptions(
        runningJoinPolicy = RunningJoinPolicy.PLAYER
    )

    override fun customizeWorldCreator(creator: WorldCreator) {
        creator.type(WorldType.FLAT)
    }

    override fun customizeEventWorld(world: World) {
        world.worldBorder.size = 25.0
        world.setGameRule(GameRules.PVP, false)
        world.setGameRule(GameRules.SPAWN_MOBS, false)
        world.difficulty = Difficulty.PEACEFUL
    }

    context(context: GameContext)
    override suspend fun onStarted() {
        effectJob?.cancel()
        effectJob = plugin.scope.runAtFixedRate(1.seconds) {
            try {
                forEachPlayerInRegion(plugin, { player ->
                    player.addPotionEffect(PotionEffectType.SATURATION.createEffect(40, 10).withParticles(false))
                })
            } catch (e: Throwable) {
                if (e is CancellationException) ensureActive()
                plugin.componentLogger.error("Failed to play effect.", e)
            }
        }
    }

    context(context: GameContext)
    override suspend fun onStop(reason: GameStopReason) {
        effectJob?.cancel()
    }
}