package dev.slne.surf.survival.events.freebuild

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.launch
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.core.api.paper.util.surfPlayer
import dev.slne.surf.npc.api.dsl.npc
import dev.slne.surf.npc.api.event.NpcInteractEvent
import dev.slne.surf.npc.api.npc.skin.NpcSkin
import dev.slne.surf.npc.api.npc.skin.NpcSkinPart
import dev.slne.surf.survival.events.freebuild.command.reloadFreebuildSurvivalEventsConfigCommand
import dev.slne.surf.survival.events.freebuild.command.switchFreebuildSurvivalServerEventsServerNpcCommand
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.time.Duration.Companion.seconds

val plugin get() = JavaPlugin.getPlugin(PaperMain::class.java)

private const val NPC_UNIQUE_NAME = "survival_events_freebuild_server_npc"
private val npcCooldown = Caffeine.newBuilder()
    .expireAfterWrite(2.seconds)
    .build<UUID, Unit>()

class PaperMain : SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {
        switchFreebuildSurvivalServerEventsServerNpcCommand()
        reloadFreebuildSurvivalEventsConfigCommand()
    }

    override suspend fun onEnableAsync() {
        npc {
            uniqueName = NPC_UNIQUE_NAME
            displayName {
                variableValue("Arty".toSmallCaps(), TextDecoration.BOLD)
            }
            uniqueName = "survival_events"
            location = Location(Bukkit.getWorlds().first(), -4.5, 73.0, 8.5, -140f, 0f)
            type = EntityType.MANNEQUIN
            skin = NpcSkin(
                "Events",
                "ewogICJ0aW1lc3RhbXAiIDogMTc3OTgwNTQ5MjIxMSwKICAicHJvZmlsZUlkIiA6ICJhZTg3MzEyNjBmMzY0ZWE2YjU3YTRkYjI5Mjk1YTA1OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdW50aW1lX0ZveHlfMTkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmM5YjVhZDcxYjk0NDQ3OGM0MDZiMGMwOGExMmQ1YTBlZjVhMjFjYjAzMDc3MzM1N2MwYjMyNWM5YTA3YWJjOSIKICAgIH0KICB9Cn0=",
                "sZ/frBUHCIx0J2zeBy4YrtqhOH0qPbBOqIpR01u9jqiziYByGh/GpwjbJN3HyS4xkP5wEEYJpj8l1+Bjsb+Vil6ZoimUdOD/zg+dIM1U7r5OCZxL3Pj5M0edm5RCx8rVvm60BI20mkAlUJ1NWx+2zyuThMnJKPHukWrTFnUm0EW16HqVBHrxQQ4uq04NHBI+tWW1GQ27Hz6PW+WxWiibyzI33ld+n6BjNoxcXOAi3o9TBkOCSV2VmGch48YhuDG3c5oNN4s0E2/wvvFnoacQUYgV3U+Xeaa+Go9TUbAy7cYFlG2jCAp6fXYRJPhXaBZywAbPqOIrYWabGJaFo0l83WzWnOU2Sva+7a/7QErgUZHzGrFaObh9FxwKcBw2ZDoZVRlBH4psNKSRnX3Vs2v945k3fZY3SAxE5C3M3hXwsb4HAWbd+CGCrOpJnkAgvK6OXudJURQDs2pUCIuhG4bUJhB7vL04Wm3n7EzujMM4lLTsK2u2Oq/dFEP99vN7KjqCjczCdKtUEcZDiOrsnWUWDOrtxnTXsSnXHviAxqahX3Oj/mG3ZgehmynG4ilXa+VN3SAu2QAmycxecAJ5I4hdGHN1FclB4wX+G1M/1Qu5pSRs6niRrF5uoVq6uH0GgLOfqQJLYsChpHEQ98Ivw9fYsvg6TB5hMtbR9teU9hBcuYY=",
                NpcSkinPart.entries.toObjectSet()
            )

            withEventHandler<NpcInteractEvent> { event ->
                if (event.npc.uniqueName != NPC_UNIQUE_NAME) {
                    return@withEventHandler
                }

                if (!FreebuildPartConfig.getConfig().enableSurvivalEventsNpc) {
                    event.player.sendText {
                        appendErrorPrefix()
                        error("Derzeit findet kein Survival Event statt. Bitte versuche es später erneut.")
                    }
                    return@withEventHandler
                }

                val surfServer =
                    SurfCoreApi.getServerByName(FreebuildPartConfig.getConfig().eventServerName)

                if (surfServer == null) {
                    event.player.sendText {
                        appendErrorPrefix()
                        error("Der Survival Events Server ist derzeit nicht erreichbar. Bitte versuche es später erneut.")
                    }
                    return@withEventHandler
                }

                if (npcCooldown.getIfPresent(event.player.uniqueId) != null) {
                    event.player.sendText {
                        appendErrorPrefix()
                        error("Bitte warte einen Moment bevor du erneut mit Arty interagierst.")
                    }
                    return@withEventHandler
                }

                plugin.launch {
                    val surfPlayer = event.player.surfPlayer
                    val result = SurfCoreApi.sendPlayerAwaiting(surfPlayer, surfServer)

                    if (!result.isSuccessful()) {
                        event.player.sendText {
                            appendErrorPrefix()
                            error("Beim Verbinden zum Survival Event Server ist ein Fehler aufgetreten. Bitte versuche es später erneut. (${result.status})")
                        }
                    }

                    npcCooldown.put(event.player.uniqueId, Unit)
                }
            }
        }
    }
}