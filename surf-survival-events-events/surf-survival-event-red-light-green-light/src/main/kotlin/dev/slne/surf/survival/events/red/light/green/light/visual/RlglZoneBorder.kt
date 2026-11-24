package dev.slne.surf.survival.events.red.light.green.light.visual

import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.survival.events.red.light.green.light.config.RlglConfig
import dev.slne.surf.survival.events.red.light.green.light.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

object RlglZoneBorder {

    private val START_ZONE_DUST = Particle.DustOptions(Color.LIME, 1f)
    private val FINISH_ZONE_DUST = Particle.DustOptions(Color.RED, 1f)
    private val PLAY_AREA_DUST = Particle.DustOptions(Color.AQUA, 1f)

    private const val PERIMETER_STEP = 0.5

    @Volatile
    private var job: Job? = null

    private val toggleLock = Any()

    @Volatile
    private var startZoneEnabled = true

    @Volatile
    private var finishZoneEnabled = true

    @Volatile
    private var playAreaEnabled = true

    fun toggleStartZone(): Boolean = synchronized(toggleLock) {
        startZoneEnabled = !startZoneEnabled
        startZoneEnabled
    }

    fun toggleFinishZone(): Boolean = synchronized(toggleLock) {
        finishZoneEnabled = !finishZoneEnabled
        finishZoneEnabled
    }

    fun togglePlayArea(): Boolean = synchronized(toggleLock) {
        playAreaEnabled = !playAreaEnabled
        playAreaEnabled
    }

    fun start(recipients: () -> Collection<Player>) {
        stop()

        job = plugin.launch {
            while (isActive) {
                val config = RlglConfig.getConfig()
                show(recipients(), config.start, config.finish)
                delay(500.milliseconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun show(recipients: Collection<Player>, start: BoundingBox, finish: BoundingBox) {
        val playArea = if (playAreaEnabled) computePlayAreaCorridor(start, finish) else null

        recipients.forEach { player ->
            if (startZoneEnabled) draw(player, start, START_ZONE_DUST)
            if (finishZoneEnabled) draw(player, finish, FINISH_ZONE_DUST)
            playArea?.let { draw(player, it, PLAY_AREA_DUST) }
        }
    }

    private fun computePlayAreaCorridor(start: BoundingBox, finish: BoundingBox): BoundingBox? {
        val startCenterX = (start.minX + start.maxX) / 2.0
        val startCenterZ = (start.minZ + start.maxZ) / 2.0
        val finishCenterX = (finish.minX + finish.maxX) / 2.0
        val finishCenterZ = (finish.minZ + finish.maxZ) / 2.0

        val separatedAlongX = abs(finishCenterX - startCenterX) >= abs(finishCenterZ - startCenterZ)

        return if (separatedAlongX) {
            val (near, far) = if (startCenterX <= finishCenterX) start to finish else finish to start
            val corridorMinX = near.maxX + 1.0
            val corridorMaxX = far.minX - 1.0
            if (corridorMinX > corridorMaxX) return null

            BoundingBox(
                corridorMinX, 0.0, minOf(start.minZ, finish.minZ),
                corridorMaxX, 1.0, maxOf(start.maxZ, finish.maxZ)
            )
        } else {
            val (near, far) = if (startCenterZ <= finishCenterZ) start to finish else finish to start
            val corridorMinZ = near.maxZ + 1.0
            val corridorMaxZ = far.minZ - 1.0
            if (corridorMinZ > corridorMaxZ) return null

            BoundingBox(
                minOf(start.minX, finish.minX), 0.0, corridorMinZ,
                maxOf(start.maxX, finish.maxX), 1.0, corridorMaxZ
            )
        }
    }

    private fun draw(player: Player, box: BoundingBox, dust: Particle.DustOptions) {
        val world = player.world
        val minX = box.minX
        val minZ = box.minZ
        val maxX = box.maxX + 1.0
        val maxZ = box.maxZ + 1.0

        perimeterPoints(minX, minZ, maxX, maxZ).forEach { (x, z) ->
            val y = world.getHighestBlockYAt(x.toInt(), z.toInt()) + 1.2
            player.spawnParticle(Particle.DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0, dust)
        }
    }

    private fun perimeterPoints(minX: Double, minZ: Double, maxX: Double, maxZ: Double): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()

        var x = minX
        while (x <= maxX) {
            points += x to minZ
            points += x to maxZ
            x += PERIMETER_STEP
        }

        var z = minZ + PERIMETER_STEP
        while (z < maxZ) {
            points += minX to z
            points += maxX to z
            z += PERIMETER_STEP
        }

        return points
    }
}