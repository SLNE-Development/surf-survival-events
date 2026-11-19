package dev.slne.surf.survival.events.hideandseek.game

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.buildLore
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.builder.meta
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekTheme
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekThemes
import dev.slne.surf.survival.events.hideandseek.util.formatLongDuration
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import io.papermc.paper.util.Tick
import kotlinx.coroutines.withContext
import net.kyori.adventure.util.Ticks
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import java.time.Duration as JavaDuration
import kotlin.time.Duration

import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

object HideAndSeekItems {
    val specialItemKey = NamespacedKey(plugin, "special_item")

    /** Marks the seekers' sword, whose material depends on the active theme. */
    val catchWeaponKey = NamespacedKey(plugin, "catch_weapon")

    const val GLOW_ITEM_ID = "glow"
    const val SHRINK_ITEM_ID = "shrink"

    private const val SHRINK_ITEM_ATTACK_DAMAGE = 7.0

    /** Shared cooldown of the seeker special items, one entry per item id. */
    private val cooldowns = Caffeine.newBuilder()
        .expireAfter(Expiry.creating<String, Unit> { _, _ -> JavaDuration.ZERO })
        .build<String, Unit>()

    private val expiration = cooldowns.policy().expireVariably().orElseThrow()

    fun resetCooldowns() {
        cooldowns.invalidateAll()
    }

    /** Starts the cooldown of [id] and returns true, or returns false while it is still running. */
    private fun tryStartCooldown(id: String, cooldown: Duration): Boolean {
        return expiration.putIfAbsent(id, Unit, cooldown.toJavaDuration()) == null
    }

    private fun remainingCooldown(id: String): Duration {
        return expiration.getExpiresAfter(id).orElse(JavaDuration.ZERO).toKotlinDuration()
    }

    private fun specialItemCooldown(): Duration {
        return HideAndSeekConfig.getConfig().gameplay.specialItemCooldown.asDuration()
    }

    suspend fun giveSeekerKit(player: Player) = withContext(plugin.entityDispatcher(player)) {
        val items = HideAndSeekThemes.theme.items
        val armorColor = Color.fromRGB(items.armorColor.value())

        val sword = themedItem(HideAndSeekThemes.material(items.swordMaterial, Material.NETHERITE_SWORD), items.sword) {
            meta {
                isUnbreakable = true
            }
            editPersistentDataContainer {
                it.set(catchWeaponKey, PersistentDataType.BOOLEAN, true)
            }
        }
        val bow = themedItem(Material.BOW, items.bow) {
            meta {
                isUnbreakable = true
            }
            addEnchantment(Enchantment.INFINITY, Enchantment.INFINITY.maxLevel)
        }
        val arrow = themedItem(Material.ARROW, items.arrow)

        fun armor(material: Material, theme: HideAndSeekTheme.ItemTheme) = themedItem(material, theme) {
            meta<LeatherArmorMeta> {
                setColor(armorColor)
                isUnbreakable = true
            }
        }

        val helmet = armor(Material.LEATHER_HELMET, items.helmet)
        val chestplate = armor(Material.LEATHER_CHESTPLATE, items.chestplate)
        val leggings = armor(Material.LEATHER_LEGGINGS, items.leggings)
        val boots = armor(Material.LEATHER_BOOTS, items.boots)

        val glowItem = themedItem(HideAndSeekThemes.material(items.glowMaterial, Material.GLOW_INK_SAC), items.glow) {
            editPersistentDataContainer {
                it.set(specialItemKey, PersistentDataType.STRING, GLOW_ITEM_ID)
            }
        }

        val shrinkItem = themedItem(HideAndSeekThemes.material(items.shrinkMaterial, Material.WHITE_TULIP), items.shrink) {
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
        remainingCooldown(GLOW_ITEM_ID).takeIf { it > Duration.ZERO }?.let {
            player.setCooldown(glowItem, Tick.tick().fromDuration(it.toJavaDuration()))
        }
    }

    /** Builds an item named and described by [theme]. */
    private fun themedItem(
        material: Material,
        theme: HideAndSeekTheme.ItemTheme,
        extra: ItemStack.() -> Unit = {}
    ): ItemStack {
        val textColor = HideAndSeekThemes.theme.textColor
        val valueColor = HideAndSeekThemes.theme.valueColor
        val name = theme.name.colorIfAbsent(valueColor)
        val lore = theme.lore.map { it.colorIfAbsent(textColor) }

        return buildItem(material) {
            displayName { append(name) }
            buildLore {
                lore.forEach { loreLine -> line { append(loreLine) } }
            }
            extra()
        }
    }

    fun specialItemId(item: ItemStack?): String? {
        return item?.persistentDataContainer?.get(specialItemKey, PersistentDataType.STRING)
    }

    fun isCatchWeapon(item: ItemStack?): Boolean {
        if (item == null) return false
        return item.persistentDataContainer.has(catchWeaponKey) || specialItemId(item) == SHRINK_ITEM_ID
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
            HideAndSeekThemes.sendRaw(player, HideAndSeekThemes.theme.messages.itemUnavailable)
            return false
        }

        val cooldown = specialItemCooldown()
        if (!tryStartCooldown(id, cooldown)) return false

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

        val duration = HideAndSeekConfig.getConfig().gameplay.glowEffectDuration.asDuration()
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
            HideAndSeekThemes.send(
                seeker,
                HideAndSeekThemes.theme.messages.glowUsed,
                HideAndSeekThemes.value("time", formatLongDuration(duration.inWholeSeconds))
            )
        }
    }
}
