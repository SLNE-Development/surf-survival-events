package dev.slne.surf.survival.events.hideandseek.theme

import dev.slne.surf.api.core.minimessage.miniMessage
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

/** Parses a MiniMessage [text] with the Surf MiniMessage instance (supports `<primary>` etc.). */
internal fun mm(text: String): Component = miniMessage.deserialize(text)

/** Builds an [HideAndSeekTheme.ItemTheme] from a MiniMessage [name] and [lore] lines. */
internal fun themeItem(name: String, vararg lore: String) =
    HideAndSeekTheme.ItemTheme(mm(name), lore.map(::mm).toMutableList())

/**
 * Everything players see and hear during hide and seek: texts, colors, item names and sounds.
 *
 * A theme lives in its own file under `themes/<name>.yml`; `config.yml` selects the active one.
 * Texts are MiniMessage. Texts without placeholders are components, which the config API reads and
 * writes as MiniMessage. Texts with placeholders such as `<time>` stay strings and are rendered
 * when they are sent. The defaults of this class are the regular hide and seek look.
 */
@ConfigSerializable
data class HideAndSeekTheme(
    @param:Comment("Chat prefix in MiniMessage. Leave empty to use the server's default prefix.")
    var prefix: Component = Component.empty(),

    @param:Comment("Default color of chat texts that do not set a color themselves.")
    var textColor: TextColor = TextColor.color(0x97B3F7),

    @param:Comment("Color of values such as times and counts.")
    var valueColor: TextColor = TextColor.color(0xF9C353),

    var roles: RolesTheme = RolesTheme(),
    var phases: PhasesTheme = PhasesTheme(),
    var scoreboard: ScoreboardTheme = ScoreboardTheme(),
    var messages: MessagesTheme = MessagesTheme(),
    var items: ItemsTheme = ItemsTheme(),
    var sounds: SoundsTheme = SoundsTheme(),
    var celebration: CelebrationTheme = CelebrationTheme()
) {
    @ConfigSerializable
    data class RoleTheme(
        var name: String = "",
        var plural: String = "",
        @param:Comment("Indefinite article used in \"Du bist jetzt <article> <role>!\", e.g. ein / eine.")
        var article: String = "ein",
        var color: TextColor = TextColor.color(0xFFFFFF)
    )

    @ConfigSerializable
    data class RolesTheme(
        var hider: RoleTheme = RoleTheme("Verstecker", "Verstecker", "ein", TextColor.color(0x3498DB)),
        var seeker: RoleTheme = RoleTheme("Sucher", "Sucher", "ein", TextColor.color(0xE74C3C)),
        var spectator: RoleTheme = RoleTheme("Zuschauer", "Zuschauer", "ein", TextColor.color(0x9B59B6)),
        @param:Comment("Shown as role while a player has no role yet.")
        var noRole: String = "Lobby"
    )

    @ConfigSerializable
    data class PhasesTheme(
        var idle: String = "Warten",
        var lobby: String = "Lobby",
        var hiding: String = "Verstecken",
        var seeking: String = "Suche",
        var celebration: String = "Ende"
    )

    @ConfigSerializable
    data class ScoreboardLine(
        @param:Comment("Symbol in front of the line (MiniMessage). Uncolored symbols use the accent or role color.")
        var icon: Component = Component.text("●"),
        var label: Component = Component.empty()
    )

    @ConfigSerializable
    data class ScoreboardTheme(
        var title: Component = mm("<#F9C353><b>HIDE </b><#3B92D1><b>& </b><#F9C353><b>SEEK"),
        @param:Comment("Color of uncolored icons on lines that are not tied to a role.")
        var accentColor: TextColor = TextColor.color(0x3B92D1),
        @param:Comment("Color of uncolored labels.")
        var labelColor: TextColor = TextColor.color(0x97B3F7),
        var phase: ScoreboardLine = ScoreboardLine(label = Component.text("Phase")),
        var time: ScoreboardLine = ScoreboardLine(label = Component.text("Zeit")),
        var role: ScoreboardLine = ScoreboardLine(label = Component.text("Rolle")),
        var hiders: ScoreboardLine = ScoreboardLine(label = Component.text("Verstecker")),
        var seekers: ScoreboardLine = ScoreboardLine(label = Component.text("Sucher")),
        var players: ScoreboardLine = ScoreboardLine(label = Component.text("Spieler")),
        @param:Comment("Extra lines at the bottom of the scoreboard (MiniMessage). Empty list = none.")
        var footer: MutableList<Component> = mutableListOf()
    )

    @ConfigSerializable
    data class MessagesTheme(
        var allInLobby: Component = Component.text("Alle Spieler sind in der Lobby!"),
        @param:Comment("Chat lines sent when the game starts.")
        var gameStartChat: MutableList<Component> = mutableListOf(
            Component.empty(),
            mm("<#3B92D1><b>HIDE AND SEEK"),
            Component.empty(),
            Component.text("Macht euch bereit – gleich geht's los!"),
            Component.empty()
        ),
        var gameStartTitle: Component = mm("<#3B92D1><b>HIDE AND SEEK"),
        var gameStartSubtitle: Component = mm("<#F9C353>Das Spiel beginnt!"),
        @param:Comment("Placeholders available in the following messages: <hiders>, <seekers>, <hider>, <seeker> and the ones named per message. <time>")
        var lobbyCountdown: String = "Das Spiel beginnt in <time>.",
        @param:Comment("<time>")
        var hidingStarted: String = "Los geht's! Die <hiders> haben <time> Zeit, um sich zu verstecken.",
        @param:Comment("<time>")
        var hidingCountdown: String = "Noch <time>, dann ziehen die <seekers> los!",
        var seekersReleased: String = "Die Vorbereitungszeit ist vorbei – die <seekers> ziehen los!",
        @param:Comment("<time>")
        var seekingCountdown: String = "Noch <time> bis zum Spielende.",
        @param:Comment("<article>, <role>")
        var roleAssigned: String = "Du bist jetzt <article> <role>!",
        @param:Comment("<player>, <killer>")
        var caught: String = "<player> wurde von <killer> gefangen!",
        @param:Comment("<player>")
        var eliminated: String = "<player> ist ausgeschieden!",
        @param:Comment("Appended to caught/eliminated while hiders remain. <count>")
        var hidersRemaining: String = " Noch <count> <hiders> übrig.",
        var hidersWin: String = "Spielende! <b><hiders> haben gewonnen!</b>",
        var seekersWin: String = "Spielende! <b><seekers> haben gewonnen!</b>",
        @param:Comment("<time>")
        var glowUsed: String = "Die <hiders> sind nun für <time> sichtbar.",
        var itemUnavailable: Component = mm("<#E74C3C>Du kannst das Item gerade nicht benutzen.")
    )

    @ConfigSerializable
    data class ItemTheme(
        var name: Component = Component.empty(),
        var lore: MutableList<Component> = mutableListOf()
    )

    @ConfigSerializable
    data class ItemsTheme(
        @param:Comment("Material of the seekers' catch weapon.")
        var swordMaterial: String = "NETHERITE_SWORD",
        var sword: ItemTheme = themeItem(
            "Schwert des Suchers",
            "<gray>Nutze dieses Schwert, um die versteckten",
            "<gray>Spieler aufzuspüren und zu fangen.",
            "Es ist unzerstörbar und dein treuer Begleiter."
        ),
        var bow: ItemTheme = themeItem(
            "Bogen des Jägers",
            "<gray>Nutze den Bogen, um flüchtende Spieler",
            "<gray>aus der Ferne aufzuhalten.",
            "Unendlich haltbar, für unendliche Jagd."
        ),
        var arrow: ItemTheme = themeItem(
            "Pfeil der Unendlichkeit",
            "<gray>Ein einfacher Pfeil, der niemals ausgeht.",
            "Nutze ihn weise und treffsicher."
        ),
        @param:Comment("Color of the seekers' leather armor.")
        var armorColor: TextColor = TextColor.color(0xF38CA8),
        var helmet: ItemTheme = themeItem(
            "Helm des Aufspürers",
            "<gray>Dieser Helm schützt dich",
            "<gray>und unterstützt dich bei der Jagd nach Versteckten.",
            "Leicht, robust und zuverlässig."
        ),
        var chestplate: ItemTheme = themeItem(
            "Brustplatte des Suchers",
            "<gray>Verleiht dir Schutz und Stärke bei",
            "<gray>deiner Mission, alle Spieler zu finden.",
            "Sie wird niemals versagen."
        ),
        var leggings: ItemTheme = themeItem(
            "Hose des Suchers",
            "<gray>Mit dieser Hose bewegst du dich geschmeidig",
            "<gray>durch jedes Terrain.",
            "Robust und zuverlässig für die Jagd."
        ),
        var boots: ItemTheme = themeItem(
            "Stiefel des Fährtenlesers",
            "<gray>Geräuschlos und leicht, ideal um dich deinen",
            "<gray>Gegnern unbemerkt zu nähern.",
            "Die perfekten Schuhe für den Sucher."
        ),
        var glowMaterial: String = "GLOW_INK_SAC",
        var glow: ItemTheme = themeItem(
            "Leuchtbeutel",
            "<gray>Lässt alle Verstecker für kurze Zeit leuchten.",
            "Hilft dir, versteckte Spieler aufzuspüren."
        ),
        var shrinkMaterial: String = "WHITE_TULIP",
        var shrink: ItemTheme = themeItem(
            "Schrumpfblume",
            "<gray>Solange du diese Blume in der Hand hältst,",
            "<gray>schrumpfst du auf die Größe der Verstecker.",
            "Du kannst mit ihr auch Verstecker fangen.",
            "Wechsle das Item, um wieder zu wachsen."
        )
    )

    @ConfigSerializable
    data class ThemeSound(
        @param:Comment("Sound key, e.g. block.bell.use. Set the volume to 0 to mute.")
        var key: Key = Key.key("block.note_block.pling"),
        @param:Comment("master, music, record, weather, block, hostile, neutral, player, ambient, voice, ui")
        var source: Sound.Source = Sound.Source.MASTER,
        var volume: Float = 1f,
        var pitch: Float = 1f
    )

    @ConfigSerializable
    data class SoundsTheme(
        var gameStart: ThemeSound = ThemeSound(Key.key("entity.ender_dragon.growl"), Sound.Source.HOSTILE, .8f, 1.2f),
        var hidingStarted: ThemeSound = ThemeSound(Key.key("entity.ender_dragon.growl"), Sound.Source.HOSTILE, .5f, .75f),
        var seekersReleased: ThemeSound = ThemeSound(Key.key("entity.ender_dragon.growl"), Sound.Source.HOSTILE, .75f, .75f),
        var gameEnd: ThemeSound = ThemeSound(Key.key("entity.ender_dragon.death"), Sound.Source.HOSTILE, .75f, .75f),
        @param:Comment("Played every second during the last 10 seconds of a countdown.")
        var countdownTick: ThemeSound = ThemeSound(Key.key("block.note_block.pling"), Sound.Source.BLOCK, .5f, 2f),
        var roleAssigned: ThemeSound = ThemeSound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, .75f, 1f),
        var teleport: ThemeSound = ThemeSound(Key.key("entity.enderman.teleport"), Sound.Source.HOSTILE, .5f, 1f)
    )

    @ConfigSerializable
    data class CelebrationTheme(
        @param:Comment("Launch fireworks at every player while the winners are celebrated.")
        var fireworks: Boolean = false,
        var fireworkColors: MutableList<TextColor> = mutableListOf(TextColor.color(0xFFFFFF)),
        @param:Comment("Show heart particles around every player while the winners are celebrated.")
        var hearts: Boolean = false,
        @param:Comment("Seconds between two bursts of fireworks/hearts.")
        var intervalSeconds: Int = 3
    )

    companion object {
        private const val PINK = "#F7A8D8"
        private const val ROYAL_BLUE = "#3B5BDB"
        private const val GOLD = "#F5C542"
        private const val LAVENDER = "#C9A0FF"
        private const val CREAM = "#FFF4E6"

        private fun color(hex: String) = TextColor.fromHexString(hex)!!

        /** Princess and castle theme for the CHARITY ROYALE event. */
        fun charityRoyale() = HideAndSeekTheme(
            prefix = mm("<$GOLD>♛ <$PINK>Schloss <dark_gray>» "),
            textColor = color(CREAM),
            valueColor = color(GOLD),
            roles = RolesTheme(
                hider = RoleTheme("Prinzessin", "Prinzessinnen", "eine", color(PINK)),
                seeker = RoleTheme("Schlosswache", "Schlosswache", "eine", color(ROYAL_BLUE)),
                spectator = RoleTheme("Gast", "Gäste", "ein", color(LAVENDER)),
                noRole = "Im Thronsaal"
            ),
            phases = PhasesTheme(
                idle = "Warten",
                lobby = "Im Thronsaal",
                hiding = "Tore schließen sich",
                seeking = "Die Wache sucht",
                celebration = "Festbankett"
            ),
            scoreboard = ScoreboardTheme(
                title = mm("<$GOLD>♛ <gradient:$PINK:$GOLD><b>Versteckspiel im Schloss</b></gradient> <$GOLD>♛"),
                accentColor = color(GOLD),
                labelColor = color(CREAM),
                phase = ScoreboardLine(mm("✿"), mm("Phase")),
                time = ScoreboardLine(mm("⌛"), mm("Zeit")),
                role = ScoreboardLine(mm("♛"), mm("Rolle")),
                hiders = ScoreboardLine(mm("❀"), mm("Prinzessinnen")),
                seekers = ScoreboardLine(mm("⚔"), mm("Schlosswache")),
                players = ScoreboardLine(mm("<$LAVENDER>♥"), mm("Gäste")),
                footer = mutableListOf(
                    mm("<gradient:$PINK:$GOLD><b>♥ Floweryalina × CHARITY ROYALE ♥</b></gradient>"),
                    mm("<$CREAM>charityroyale.at/wishmaker/floweryalina")
                )
            ),
            messages = MessagesTheme(
                allInLobby = mm("Alle Gäste haben sich im Thronsaal versammelt!"),
                gameStartChat = mutableListOf(
                    Component.empty(),
                    mm("<$GOLD>♛ <gradient:$PINK:$GOLD><b>Hört, hört!</b></gradient> <$GOLD>♛"),
                    Component.empty(),
                    mm("Das große Versteckspiel im Schloss beginnt gleich!"),
                    Component.empty()
                ),
                gameStartTitle = mm("<$GOLD>♛ <gradient:$PINK:$GOLD><b>Hört, hört!</b></gradient> <$GOLD>♛"),
                gameStartSubtitle = mm("<$CREAM>Das Versteckspiel beginnt"),
                lobbyCountdown = "Die Tore öffnen sich in <time>.",
                hidingStarted = "Die Tore schließen sich! Die <hiders> haben <time>, um sich im Schloss zu verstecken.",
                hidingCountdown = "Noch <time>, dann zieht die <seekers> los!",
                seekersReleased = "Die <seekers> zieht los – versteckt euch gut, <hiders>!",
                seekingCountdown = "Noch <time> bis zum Festbankett.",
                roleAssigned = "Du bist jetzt <article> <role>!",
                caught = "<player> wurde von der Wache <killer> entdeckt!",
                eliminated = "<player> hat das Spiel verlassen!",
                hidersRemaining = " Noch <count> <hiders> versteckt.",
                hidersWin = "<b><hiders> sind entkommen!</b> <$GOLD>♛",
                seekersWin = "<b>Die <seekers> hat alle gefunden!</b> <$GOLD>⚔",
                glowUsed = "Feenstaub! Die <hiders> leuchten nun für <time>.",
                itemUnavailable = mm("<#E74C3C>Das kannst du gerade nicht benutzen.")
            ),
            items = ItemsTheme(
                swordMaterial = "GOLDEN_SWORD",
                sword = themeItem(
                    "<$GOLD>Ritterschwert",
                    "<gray>Ein Hieb, und die Prinzessin",
                    "<gray>ist entdeckt.",
                    "Geschmiedet für die Schlosswache."
                ),
                bow = themeItem(
                    "<$GOLD>Bogen der Schlosswache",
                    "<gray>Halte flüchtende Prinzessinnen",
                    "<gray>aus der Ferne auf."
                ),
                arrow = themeItem("<$GOLD>Königlicher Pfeil", "<gray>Geht niemals aus."),
                armorColor = color(ROYAL_BLUE),
                helmet = themeItem("<$ROYAL_BLUE>Helm der Schlosswache", "<gray>Im Dienste der Krone."),
                chestplate = themeItem("<$ROYAL_BLUE>Wappenrock der Schlosswache", "<gray>Im Dienste der Krone."),
                leggings = themeItem("<$ROYAL_BLUE>Beinschutz der Schlosswache", "<gray>Im Dienste der Krone."),
                boots = themeItem("<$ROYAL_BLUE>Stiefel der Schlosswache", "<gray>Leise über jeden Marmorboden."),
                glowMaterial = "GLOWSTONE_DUST",
                glow = themeItem("<$PINK>Feenstaub", "<gray>Lässt alle Prinzessinnen für kurze Zeit leuchten."),
                shrinkMaterial = "PINK_TULIP",
                shrink = themeItem(
                    "<$PINK>Zauberblume",
                    "<gray>Solange du sie in der Hand hältst,",
                    "<gray>schrumpfst du auf Prinzessinnengröße.",
                    "Auch mit ihr kannst du Prinzessinnen fangen.",
                    "Wechsle das Item, um wieder zu wachsen."
                )
            ),
            sounds = SoundsTheme(
                gameStart = ThemeSound(Key.key("block.bell.use"), Sound.Source.MASTER, 1f, 1f),
                hidingStarted = ThemeSound(Key.key("block.bell.resonate"), Sound.Source.MASTER, .8f, 1.2f),
                seekersReleased = ThemeSound(Key.key("event.raid.horn"), Sound.Source.MASTER, .6f, 1.4f),
                gameEnd = ThemeSound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 1f),
                countdownTick = ThemeSound(Key.key("block.amethyst_block.chime"), Sound.Source.BLOCK, 1f, 1.2f),
                roleAssigned = ThemeSound(Key.key("entity.player.levelup"), Sound.Source.PLAYER, .75f, 1.2f),
                teleport = ThemeSound(Key.key("block.amethyst_cluster.break"), Sound.Source.PLAYER, .8f, 1.2f)
            ),
            celebration = CelebrationTheme(
                fireworks = true,
                fireworkColors = mutableListOf(color(PINK), color(GOLD), color(LAVENDER)),
                hearts = true,
                intervalSeconds = 3
            )
        )
    }
}
