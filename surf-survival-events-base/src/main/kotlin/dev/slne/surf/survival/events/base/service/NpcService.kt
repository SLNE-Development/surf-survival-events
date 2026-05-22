package dev.slne.surf.survival.events.base.service

import dev.slne.surf.api.core.font.toSmallCaps
import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.core.util.toObjectSet
import dev.slne.surf.npc.api.dsl.npc
import dev.slne.surf.npc.api.npc.Npc
import dev.slne.surf.npc.api.npc.skin.NpcSkin
import dev.slne.surf.npc.api.npc.skin.NpcSkinPart
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType

object NpcService {

    val npcs = mutableObject2ObjectMapOf<String, Npc>()

    fun showNpc() {
        val npc = npc {
            displayName {
                variableValue("Event Manager".toSmallCaps(), TextDecoration.BOLD)
            }
            uniqueName = "survival_events"
            location = Location(Bukkit.getWorld("world"), -12.5, 79.0, 15.5, -140f, 0f)
            type = EntityType.MANNEQUIN
            skin = NpcSkin(
                "Events",
                "ewogICJ0aW1lc3RhbXAiIDogMTc0MzYxNDkxNzg1MywKICAicHJvZmlsZUlkIiA6ICJjNWVmOGQ1NDIwOWY0OTdlYWYzYzA1NjA3MjZhYTMwNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaXNoX0RheCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iZWY5NjlhZWM1ZGMwODU1MjZkYzA2ZmIxNDIzNTBmYjdjOTAwNDE1NzJiNWI1YzhjZjYxN2VlZjQwMTZhZTY1IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
                "n1BdUrbKk3ckKywVLtFW5MTiXtxCSN/4on4vdOkb7p7c1IB698KF7cg19UPtXTUMtfyb38uJkGLej4jcv/SbwQLyxGdGA70DA5VXX2hE9TFNFt5i4PYG/PVj9+FCb+S19qGccMCrWB0U080Ozt/DfzLodIhXXLlKeJCoEjmMav3x6mgFbR0WmJmNwFt58bYLSVdNWBWHx2gI45z+oNdb/Thj+ZGrlfUAK0WXyBzzXPOR8ee8otcUZovPFiRwdSPq/KEaoA2mTmnlA3WhSKSHTFCJvH1Sf2+iaCAiA/1rGfuzT1EqcE2PU3OVcvqGvF024IKchnUErO7NmS1mMW5UQ/S9Lz6kzqE+YJ/zqAuYLF+j8OByiQIc7GEwEywEYqfdMr5E/6nb3Sdgxbn9feuD97cIfVCql60t81RHAs2ZvJT8F9ExKaVS5vf+ucYArCmK+wV8jfWTAlYTc6rmUx/bF724ydlL6THAx3mWX0ZJYwjPTyvKg1w+bRTpZsxlvg68BXzqR04/jCNw2psa4KuxoUJLxtaHcAOn+VHG50T8533hlxFWXpliD3hg6HjoMSUEoCDvvFP6SRb4rRPs7W00d7c/ilogBxMcaMMa3VzcixchJEmLUe43KVwnvD6q8QTRkc+mD8rpR3XgzOSpiqD6afMWxIVtYiQxmeolE4T478s=",
                NpcSkinPart.entries.toObjectSet()
            )
        }
        npcs["survival_events"] = npc
    }
    fun hideNpc() {
        if (npcs.isEmpty()) return
        npcs["survival_events"]?.delete()
    }
}