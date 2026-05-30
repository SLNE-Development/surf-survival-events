package dev.slne.surf.survival.events.base.service

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.npc.api.dsl.npc
import dev.slne.surf.npc.api.npc.Npc
import dev.slne.surf.npc.api.npc.skin.NpcSkin
import dev.slne.surf.npc.api.npc.skin.NpcSkinPart
import dev.slne.surf.survival.events.base.config.SurfRaceConfig
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType

object NpcService {
    const val SURVIVAL_EVENTS_NPC_NAME = "survival_events"

    val npcs = mutableObject2ObjectMapOf<String, Npc>()

    fun showNpc() {
        val eventManager = SurfRaceConfig.getConfig().eventManager.firstOrNull()
        val world = Bukkit.getWorld(eventManager?.eventManagerWorld ?: "world")
            ?: Bukkit.getWorlds().first()
        val locationConfig = Location(
            world,
            eventManager?.eventManagerX ?: -12.5,
            eventManager?.eventManagerY ?: 79.0,
            eventManager?.eventManagerZ ?: 15.5,
        )

        val npc = npc {
            displayName {
                variableValue("Arty".toSmallCaps(), TextDecoration.BOLD)
            }
            uniqueName = SURVIVAL_EVENTS_NPC_NAME
            location = locationConfig
            type = EntityType.MANNEQUIN
            skin = NpcSkin(
                "Events",
                "ewogICJ0aW1lc3RhbXAiIDogMTc3OTgwNTQ5MjIxMSwKICAicHJvZmlsZUlkIiA6ICJhZTg3MzEyNjBmMzY0ZWE2YjU3YTRkYjI5Mjk1YTA1OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdW50aW1lX0ZveHlfMTkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmM5YjVhZDcxYjk0NDQ3OGM0MDZiMGMwOGExMmQ1YTBlZjVhMjFjYjAzMDc3MzM1N2MwYjMyNWM5YTA3YWJjOSIKICAgIH0KICB9Cn0=",
                "sZ/frBUHCIx0J2zeBy4YrtqhOH0qPbBOqIpR01u9jqiziYByGh/GpwjbJN3HyS4xkP5wEEYJpj8l1+Bjsb+Vil6ZoimUdOD/zg+dIM1U7r5OCZxL3Pj5M0edm5RCx8rVvm60BI20mkAlUJ1NWx+2zyuThMnJKPHukWrTFnUm0EW16HqVBHrxQQ4uq04NHBI+tWW1GQ27Hz6PW+WxWiibyzI33ld+n6BjNoxcXOAi3o9TBkOCSV2VmGch48YhuDG3c5oNN4s0E2/wvvFnoacQUYgV3U+Xeaa+Go9TUbAy7cYFlG2jCAp6fXYRJPhXaBZywAbPqOIrYWabGJaFo0l83WzWnOU2Sva+7a/7QErgUZHzGrFaObh9FxwKcBw2ZDoZVRlBH4psNKSRnX3Vs2v945k3fZY3SAxE5C3M3hXwsb4HAWbd+CGCrOpJnkAgvK6OXudJURQDs2pUCIuhG4bUJhB7vL04Wm3n7EzujMM4lLTsK2u2Oq/dFEP99vN7KjqCjczCdKtUEcZDiOrsnWUWDOrtxnTXsSnXHviAxqahX3Oj/mG3ZgehmynG4ilXa+VN3SAu2QAmycxecAJ5I4hdGHN1FclB4wX+G1M/1Qu5pSRs6niRrF5uoVq6uH0GgLOfqQJLYsChpHEQ98Ivw9fYsvg6TB5hMtbR9teU9hBcuYY=",
                NpcSkinPart.entries.toObjectSet()
            )
        }
        npcs[SURVIVAL_EVENTS_NPC_NAME] = npc
    }

    fun hideNpc() {
        if (npcs.isEmpty()) return
        npcs["survival_events"]?.delete()
    }

    fun newPositionNpc(locationConfig: Location) {
        hideNpc()

        val npc = npc {
            displayName {
                variableValue("Arty".toSmallCaps(), TextDecoration.BOLD)
            }
            uniqueName = SURVIVAL_EVENTS_NPC_NAME
            location = locationConfig
            type = EntityType.MANNEQUIN
            skin = NpcSkin(
                "Events",
                "ewogICJ0aW1lc3RhbXAiIDogMTc3OTgwNTQ5MjIxMSwKICAicHJvZmlsZUlkIiA6ICJhZTg3MzEyNjBmMzY0ZWE2YjU3YTRkYjI5Mjk1YTA1OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJGdW50aW1lX0ZveHlfMTkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmM5YjVhZDcxYjk0NDQ3OGM0MDZiMGMwOGExMmQ1YTBlZjVhMjFjYjAzMDc3MzM1N2MwYjMyNWM5YTA3YWJjOSIKICAgIH0KICB9Cn0=",
                "sZ/frBUHCIx0J2zeBy4YrtqhOH0qPbBOqIpR01u9jqiziYByGh/GpwjbJN3HyS4xkP5wEEYJpj8l1+Bjsb+Vil6ZoimUdOD/zg+dIM1U7r5OCZxL3Pj5M0edm5RCx8rVvm60BI20mkAlUJ1NWx+2zyuThMnJKPHukWrTFnUm0EW16HqVBHrxQQ4uq04NHBI+tWW1GQ27Hz6PW+WxWiibyzI33ld+n6BjNoxcXOAi3o9TBkOCSV2VmGch48YhuDG3c5oNN4s0E2/wvvFnoacQUYgV3U+Xeaa+Go9TUbAy7cYFlG2jCAp6fXYRJPhXaBZywAbPqOIrYWabGJaFo0l83WzWnOU2Sva+7a/7QErgUZHzGrFaObh9FxwKcBw2ZDoZVRlBH4psNKSRnX3Vs2v945k3fZY3SAxE5C3M3hXwsb4HAWbd+CGCrOpJnkAgvK6OXudJURQDs2pUCIuhG4bUJhB7vL04Wm3n7EzujMM4lLTsK2u2Oq/dFEP99vN7KjqCjczCdKtUEcZDiOrsnWUWDOrtxnTXsSnXHviAxqahX3Oj/mG3ZgehmynG4ilXa+VN3SAu2QAmycxecAJ5I4hdGHN1FclB4wX+G1M/1Qu5pSRs6niRrF5uoVq6uH0GgLOfqQJLYsChpHEQ98Ivw9fYsvg6TB5hMtbR9teU9hBcuYY=",
                NpcSkinPart.entries.toObjectSet()
            )
        }
        npcs[SURVIVAL_EVENTS_NPC_NAME] = npc
    }
}