package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.builder.meta
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.util.formatLongDuration
import io.papermc.paper.util.Tick
import kotlinx.coroutines.withContext
import net.kyori.adventure.util.Ticks
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object HideAndSeekItems {
    val specialItemKey = NamespacedKey(plugin, "special_item")

    const val GLOW_ITEM_ID = "glow"
    const val SHRINK_ITEM_ID = "shrink"

    val SWORD_MATERIAL = Material.NETHERITE_SWORD

    private const val SHRINK_ITEM_ATTACK_DAMAGE = 7.0

    private val lastUse = ConcurrentHashMap<String, Long>()

    fun resetCooldowns() {
        lastUse.clear()
    }

    private fun remainingCooldown(id: String, cooldown: Duration): Duration {
        val last = lastUse[id] ?: return Duration.ZERO
        return (cooldown - (System.currentTimeMillis() - last).milliseconds)
            .coerceAtLeast(Duration.ZERO)
    }

    private fun specialItemCooldown(): Duration {
        return HideAndSeekConfig.getConfig().gameplay.specialItemCooldownSeconds.seconds
    }

    suspend fun giveSeekerKit(player: Player) = withContext(plugin.entityDispatcher(player)) {
        val sword = buildItem(SWORD_MATERIAL) {
            displayName { primary("Schwert des Suchers") }
            buildLore {
                line { secondary("Nutze dieses Schwert, um die versteckten") }
                line { secondary("Spieler aufzuspüren und zu fangen.") }
                line { info("Es ist unzerstörbar und dein treuer Begleiter.") }
            }
            meta {
                isUnbreakable = true
            }
        }
        val bow = buildItem(Material.BOW) {
            displayName { primary("Bogen des Jägers") }
            buildLore {
                line { secondary("Nutze den Bogen, um flüchtende Spieler") }
                line { secondary("aus der Ferne aufzuhalten.") }
                line { info("Unendlich haltbar, für unendliche Jagd.") }
            }
            meta {
                isUnbreakable = true
            }
            addEnchantment(Enchantment.INFINITY, Enchantment.INFINITY.maxLevel)
        }
        val arrow = buildItem(Material.ARROW) {
            displayName { primary("Pfeil der Unendlichkeit") }
            buildLore {
                line { secondary("Ein einfacher Pfeil, der niemals ausgeht.") }
                line { info("Nutze ihn weise und treffsicher.") }
            }
        }

        val helmet = buildItem(Material.LEATHER_HELMET) {
            displayName { primary("Helm des Aufspürers") }
            buildLore {
                line { secondary("Dieser Helm schützt dich") }
                line { secondary("und unterstützt dich bei der Jagd nach Versteckten.") }
                line { info("Leicht, robust und zuverlässig.") }
            }
            meta<LeatherArmorMeta> {
                setColor(Color.fromRGB(243, 140, 168))
                isUnbreakable = true
            }
        }
        val chestplate = buildItem(Material.LEATHER_CHESTPLATE) {
            displayName { primary("Brustplatte des Suchers") }
            buildLore {
                line { secondary("Verleiht dir Schutz und Stärke bei") }
                line { secondary("deiner Mission, alle Spieler zu finden.") }
                line { info("Sie wird niemals versagen.") }
            }
            meta<LeatherArmorMeta> {
                setColor(Color.fromRGB(243, 140, 168))
                isUnbreakable = true
            }
        }
        val leggings = buildItem(Material.LEATHER_LEGGINGS) {
            displayName { primary("Hose des Suchers") }
            buildLore {
                line { secondary("Mit dieser Hose bewegst du dich geschmeidig") }
                line { secondary("durch jedes Terrain.") }
                line { info("Robust und zuverlässig für die Jagd.") }
            }
            meta<LeatherArmorMeta> {
                setColor(Color.fromRGB(243, 140, 168))
                isUnbreakable = true
            }
        }
        val boots = buildItem(Material.LEATHER_BOOTS) {
            displayName { primary("Stiefel des Fährtenlesers") }
            buildLore {
                line { secondary("Geräuschlos und leicht, ideal um dich deinen") }
                line { secondary("Gegnern unbemerkt zu nähern.") }
                line { info("Die perfekten Schuhe für den Sucher.") }
            }
            meta<LeatherArmorMeta> {
                setColor(Color.fromRGB(243, 140, 168))
                isUnbreakable = true
            }
        }

        val glowItem = buildItem(Material.GLOW_INK_SAC) {
            displayName { primary("Leuchtbeutel") }
            buildLore {
                line { secondary("Lässt alle Verstecker für kurze Zeit leuchten.") }
                line { info("Hilft dir, versteckte Spieler aufzuspüren.") }
            }
            editPersistentDataContainer {
                it.set(specialItemKey, PersistentDataType.STRING, GLOW_ITEM_ID)
            }
        }

        val shrinkItem = buildItem(Material.WHITE_TULIP) {
            displayName { primary("Schrumpfblume") }
            buildLore {
                line { secondary("Solange du diese Blume in der Hand hältst,") }
                line { secondary("schrumpfst du auf die Größe der Verstecker.") }
                line { info("Du kannst mit ihr auch Verstecker fangen.") }
                line { info("Wechsle das Item, um wieder zu wachsen.") }
            }
            meta {
                addAttributeModifier(
                    Attribute.ATTACK_DAMAGE,
                    AttributeModifier(
                        NamespacedKey(plugin, "shrink_attack_damage"),
                        SHRINK_ITEM_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                    )
                )
            }
            editPersistentDataContainer {
                it.set(specialItemKey, PersistentDataType.STRING, SHRINK_ITEM_ID)
            }
        }

        with(player.inventory) {
            clear()
            setItem(0, sword)
            setItem(1, bow)
            setItem(4, glowItem)
            setItem(5, shrinkItem)
            setItem(8, arrow)

            setHelmet(helmet)
            setChestplate(chestplate)
            setLeggings(leggings)
            setBoots(boots)
        }

        val cooldown = specialItemCooldown()
        remainingCooldown(GLOW_ITEM_ID, cooldown).takeIf { it > Duration.ZERO }?.let {
            player.setCooldown(glowItem, Tick.tick().fromDuration(it.toJavaDuration()))
        }
    }

    fun specialItemId(item: ItemStack?): String? {
        return item?.persistentDataContainer?.get(specialItemKey, PersistentDataType.STRING)
    }

    fun isCatchWeapon(item: ItemStack?): Boolean {
        if (item == null) return false
        return item.type == SWORD_MATERIAL || specialItemId(item) == SHRINK_ITEM_ID
    }

    suspend fun handleSpecialItem(player: Player, item: ItemStack, id: String) {
        when (id) {
            GLOW_ITEM_ID -> handleGlowItem(player, item)
        }
    }

    suspend fun updateShrinkScale(player: Player, heldItem: ItemStack?) {
        if (HideAndSeekRoleManager.roleOf(player) != SeekerRole) return

        val targetScale = shrinkTargetScale(heldItem)
        withContext(plugin.entityDispatcher(player)) {
            HideAndSeekRoleManager.applyScale(player, targetScale)
        }
    }

    suspend fun refreshShrinkScale(player: Player) {
        if (HideAndSeekRoleManager.roleOf(player) != SeekerRole) return

        withContext(plugin.entityDispatcher(player)) {
            val targetScale = shrinkTargetScale(player.inventory.itemInMainHand)
            HideAndSeekRoleManager.applyScale(player, targetScale)
        }
    }

    private fun shrinkTargetScale(heldItem: ItemStack?): Double {
        return if (HideAndSeekService.isSeekingPhase && specialItemId(heldItem) == SHRINK_ITEM_ID) {
            HideAndSeekConfig.getConfig().gameplay.hiderScale
        } else {
            SeekerRole.appliedScale()
        }
    }

    private suspend fun runPreCheck(player: Player, item: ItemStack, id: String): Boolean {
        if (!HideAndSeekService.isSeekingPhase) {
            player.sendText {
                appendErrorPrefix()
                error("Du kannst das Item gerade nicht benutzen.")
            }
            return false
        }

        val cooldown = specialItemCooldown()
        if (remainingCooldown(id, cooldown) > Duration.ZERO) return false
        lastUse[id] = System.currentTimeMillis()

        val cooldownTicks = Tick.tick().fromDuration(cooldown.toJavaDuration())
        HideAndSeekRoleManager.onlineSeekers.forEach { seeker ->
            withContext(plugin.entityDispatcher(seeker)) {
                seeker.setCooldown(item, cooldownTicks)
            }
        }

        return true
    }

    private suspend fun handleGlowItem(player: Player, item: ItemStack) {
        if (!runPreCheck(player, item, GLOW_ITEM_ID)) return

        val duration = HideAndSeekConfig.getConfig().gameplay.glowEffectDurationSeconds.seconds
        val effect = PotionEffectType.GLOWING.createEffect(
            (duration.inWholeMilliseconds / Ticks.SINGLE_TICK_DURATION_MS).toInt(),
            255
        ).withIcon(false).withParticles(false).withAmbient(false)

        HideAndSeekRoleManager.onlineHiders.forEach { hider ->
            withContext(plugin.entityDispatcher(hider)) {
                hider.addPotionEffect(effect)
            }
        }

        HideAndSeekRoleManager.onlineSeekers.forEach { seeker ->
            seeker.sendText {
                appendInfoPrefix()
                info("Die Verstecker sind nun für ")
                variableValue(formatLongDuration(duration.inWholeSeconds))
                info(" sichtbar.")
            }
        }
    }

}
