package dev.slne.surf.survival.events.hideandseek.theme

import dev.slne.surf.api.core.config.manager.SpongeConfigManager
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekRole
import dev.slne.surf.survival.events.hideandseek.game.HiderRole
import dev.slne.surf.survival.events.hideandseek.game.SeekerRole
import dev.slne.surf.survival.events.hideandseek.plugin
import dev.slne.surf.survival.events.hideandseek.theme.HideAndSeekTheme.ThemeSound
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Material
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

/**
 * Loads the theme selected in `config.yml` from `themes/<name>.yml` and renders its texts.
 *
 * The shipped presets (`default`, `charity-royale`) are written once if their file is missing, so
 * edits made on the server are never overwritten.
 */
object HideAndSeekThemes {
    const val DEFAULT_THEME = "default"
    const val CHARITY_ROYALE_THEME = "charity-royale"

    private val validName = Regex("^[a-zA-Z0-9_-]+$")
    private val miniMessage = MiniMessage.miniMessage()

    @Volatile
    var theme: HideAndSeekTheme = HideAndSeekTheme()
        private set

    @Volatile
    var themeName: String = DEFAULT_THEME
        private set

    private val folder: Path get() = plugin.dataPath.resolve("themes")

    /** Names of all theme files in the themes folder. */
    fun available(): List<String> {
        if (!folder.exists()) return emptyList()
        return folder.listDirectoryEntries("*.yml").map { it.nameWithoutExtension }.sorted()
    }

    /**
     * Writes missing presets and loads the theme named in `config.yml`.
     *
     * Returns an error message if the selected theme could not be loaded; the previous theme then
     * stays active (or the default theme on first load).
     */
    fun load(): String? {
        writePresetIfMissing(DEFAULT_THEME) { HideAndSeekTheme() }
        writePresetIfMissing(CHARITY_ROYALE_THEME) { HideAndSeekTheme.charityRoyale() }

        val name = HideAndSeekConfig.getConfig().theme.trim()
        if (!validName.matches(name)) {
            return "Ungültiger Theme-Name \"$name\" (erlaubt: a-z, 0-9, _ und -)."
        }
        if (!folder.resolve("$name.yml").exists()) {
            return "Das Theme \"$name\" existiert nicht (themes/$name.yml)."
        }

        return try {
            theme = SpongeConfigManager.yaml(HideAndSeekTheme::class.java, folder, "$name.yml").config
            themeName = name
            null
        } catch (e: Throwable) {
            plugin.componentLogger.error("Failed to load hide and seek theme $name", e)
            "Das Theme \"$name\" konnte nicht geladen werden: ${e.message}"
        }
    }

    private fun writePresetIfMissing(name: String, preset: () -> HideAndSeekTheme) {
        if (folder.resolve("$name.yml").exists()) return
        try {
            val manager = SpongeConfigManager.yaml(HideAndSeekTheme::class.java, folder, "$name.yml")
            manager.config = preset()
            manager.save()
        } catch (e: Throwable) {
            plugin.componentLogger.error("Failed to write hide and seek theme preset $name", e)
        }
    }

    // ---- rendering ------------------------------------------------------------------------------

    fun color(hex: String, fallback: TextColor = TextColor.color(0xFFFFFF)): TextColor {
        return TextColor.fromHexString(hex.trim()) ?: fallback
    }

    fun material(name: String, fallback: Material): Material {
        return Material.matchMaterial(name.trim()) ?: fallback
    }

    fun roleTheme(role: HideAndSeekRole): HideAndSeekTheme.RoleTheme = with(theme.roles) {
        when (role) {
            HiderRole -> hider
            SeekerRole -> seeker
            else -> spectator
        }
    }

    /** Renders a MiniMessage text with the role placeholders and [placeholders]. */
    fun render(text: String, vararg placeholders: TagResolver): Component {
        return miniMessage.deserialize(text, TagResolver.resolver(roleResolver(), *placeholders))
    }

    /** A placeholder that shows [value] in the theme's value color. */
    fun value(key: String, value: Any): TagResolver {
        return Placeholder.component(key, Component.text(value.toString(), color(theme.valueColor)))
    }

    /** A placeholder that shows [name] in the color of [role]. */
    fun playerName(key: String, name: String, role: HideAndSeekRole?): TagResolver {
        val roleColor = role?.let { color(roleTheme(it).color) } ?: color(theme.valueColor)
        return Placeholder.component(key, Component.text(name, roleColor))
    }

    fun roleName(role: HideAndSeekRole, bold: Boolean = false): Component {
        val roleTheme = roleTheme(role)
        val component = Component.text(roleTheme.name, color(roleTheme.color))
        return if (bold) component.decorate(TextDecoration.BOLD) else component
    }

    private fun roleResolver(): TagResolver {
        val roles = theme.roles
        return TagResolver.resolver(
            Placeholder.component("hiders", Component.text(roles.hider.plural, color(roles.hider.color))),
            Placeholder.component("seekers", Component.text(roles.seeker.plural, color(roles.seeker.color))),
            Placeholder.component("hider", Component.text(roles.hider.name, color(roles.hider.color))),
            Placeholder.component("seeker", Component.text(roles.seeker.name, color(roles.seeker.color)))
        )
    }

    /** Sends a themed chat message with the theme prefix. */
    fun send(audience: Audience, text: String, vararg placeholders: TagResolver) {
        val message = render(text, *placeholders).colorIfAbsent(color(theme.textColor))
        sendRaw(audience, message)
    }

    /** Sends an already rendered [message] with the theme prefix. */
    fun sendRaw(audience: Audience, message: Component) {
        val prefix = theme.prefix
        if (prefix.isBlank()) {
            audience.sendText {
                appendInfoPrefix()
                append(message)
            }
        } else {
            audience.sendMessage(render(prefix).append(message))
        }
    }

    fun playSound(audience: Audience, sound: ThemeSound) {
        val key = sound.key.trim()
        if (key.isEmpty()) return

        val parsedKey = runCatching { Key.key(key) }.getOrNull() ?: return
        val source = Sound.Source.NAMES.value(sound.source.trim().lowercase()) ?: Sound.Source.MASTER
        audience.playSound(Sound.sound(parsedKey, source, sound.volume, sound.pitch))
    }
}
