package dev.slne.surf.survival.events.hideandseek.theme

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

/**
 * Everything players see and hear during hide and seek: texts, colors, item names and sounds.
 *
 * A theme lives in its own file under `themes/<name>.yml`; `config.yml` selects the active one.
 * All texts are MiniMessage strings. The defaults of this class are the regular hide and seek look.
 */
@ConfigSerializable
data class HideAndSeekTheme(
    @param:Comment("Chat prefix in MiniMessage. Leave empty to use the server's default prefix.")
    var prefix: String = "",

    @param:Comment("Default color of chat texts that do not set a color themselves.")
    var textColor: String = "#97B3F7",

    @param:Comment("Color of values such as times and counts.")
    var valueColor: String = "#F9C353",

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
        var color: String = "#FFFFFF"
    )

    @ConfigSerializable
    data class RolesTheme(
        var hider: RoleTheme = RoleTheme("Verstecker", "Verstecker", "ein", "#3498DB"),
        var seeker: RoleTheme = RoleTheme("Sucher", "Sucher", "ein", "#E74C3C"),
        var spectator: RoleTheme = RoleTheme("Zuschauer", "Zuschauer", "ein", "#9B59B6"),
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
        var icon: String = "●",
        var label: String = ""
    )

    @ConfigSerializable
    data class ScoreboardTheme(
        var title: String = "<#F9C353><b>HIDE </b><#3B92D1><b>& </b><#F9C353><b>SEEK",
        @param:Comment("Color of uncolored icons on lines that are not tied to a role.")
        var accentColor: String = "#3B92D1",
        @param:Comment("Color of uncolored labels.")
        var labelColor: String = "#97B3F7",
        var phase: ScoreboardLine = ScoreboardLine(label = "Phase"),
        var time: ScoreboardLine = ScoreboardLine(label = "Zeit"),
        var role: ScoreboardLine = ScoreboardLine(label = "Rolle"),
        var hiders: ScoreboardLine = ScoreboardLine(label = "Verstecker"),
        var seekers: ScoreboardLine = ScoreboardLine(label = "Sucher"),
        var players: ScoreboardLine = ScoreboardLine(label = "Spieler"),
        @param:Comment("Extra lines at the bottom of the scoreboard (MiniMessage). Empty list = none.")
        var footer: MutableList<String> = mutableListOf()
    )

    @ConfigSerializable
    data class MessagesTheme(
        @param:Comment("Placeholders available in messages: <hiders>, <seekers>, <hider>, <seeker> and the ones named per message.")
        var allInLobby: String = "Alle Spieler sind in der Lobby!",
        @param:Comment("Chat lines sent when the game starts.")
        var gameStartChat: MutableList<String> = mutableListOf(
            "",
            "<#3B92D1><b>HIDE AND SEEK",
            "",
            "Macht euch bereit – gleich geht's los!",
            ""
        ),
        var gameStartTitle: String = "<#3B92D1><b>HIDE AND SEEK",
        var gameStartSubtitle: String = "<#F9C353>Das Spiel beginnt!",
        @param:Comment("<time>")
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
        var itemUnavailable: String = "<#E74C3C>Du kannst das Item gerade nicht benutzen."
    )

    @ConfigSerializable
    data class ItemTheme(
        var name: String = "",
        var lore: MutableList<String> = mutableListOf()
    )

    @ConfigSerializable
    data class ItemsTheme(
        @param:Comment("Material of the seekers' catch weapon.")
        var swordMaterial: String = "NETHERITE_SWORD",
        var sword: ItemTheme = ItemTheme(
            "Schwert des Suchers",
            mutableListOf(
                "<gray>Nutze dieses Schwert, um die versteckten",
                "<gray>Spieler aufzuspüren und zu fangen.",
                "Es ist unzerstörbar und dein treuer Begleiter."
            )
        ),
        var bow: ItemTheme = ItemTheme(
            "Bogen des Jägers",
            mutableListOf(
                "<gray>Nutze den Bogen, um flüchtende Spieler",
                "<gray>aus der Ferne aufzuhalten.",
                "Unendlich haltbar, für unendliche Jagd."
            )
        ),
        var arrow: ItemTheme = ItemTheme(
            "Pfeil der Unendlichkeit",
            mutableListOf("<gray>Ein einfacher Pfeil, der niemals ausgeht.", "Nutze ihn weise und treffsicher.")
        ),
        @param:Comment("Color of the seekers' leather armor.")
        var armorColor: String = "#F38CA8",
        var helmet: ItemTheme = ItemTheme(
            "Helm des Aufspürers",
            mutableListOf(
                "<gray>Dieser Helm schützt dich",
                "<gray>und unterstützt dich bei der Jagd nach Versteckten.",
                "Leicht, robust und zuverlässig."
            )
        ),
        var chestplate: ItemTheme = ItemTheme(
            "Brustplatte des Suchers",
            mutableListOf(
                "<gray>Verleiht dir Schutz und Stärke bei",
                "<gray>deiner Mission, alle Spieler zu finden.",
                "Sie wird niemals versagen."
            )
        ),
        var leggings: ItemTheme = ItemTheme(
            "Hose des Suchers",
            mutableListOf(
                "<gray>Mit dieser Hose bewegst du dich geschmeidig",
                "<gray>durch jedes Terrain.",
                "Robust und zuverlässig für die Jagd."
            )
        ),
        var boots: ItemTheme = ItemTheme(
            "Stiefel des Fährtenlesers",
            mutableListOf(
                "<gray>Geräuschlos und leicht, ideal um dich deinen",
                "<gray>Gegnern unbemerkt zu nähern.",
                "Die perfekten Schuhe für den Sucher."
            )
        ),
        var glowMaterial: String = "GLOW_INK_SAC",
        var glow: ItemTheme = ItemTheme(
            "Leuchtbeutel",
            mutableListOf("<gray>Lässt alle Verstecker für kurze Zeit leuchten.", "Hilft dir, versteckte Spieler aufzuspüren.")
        ),
        var shrinkMaterial: String = "WHITE_TULIP",
        var shrink: ItemTheme = ItemTheme(
            "Schrumpfblume",
            mutableListOf(
                "<gray>Solange du diese Blume in der Hand hältst,",
                "<gray>schrumpfst du auf die Größe der Verstecker.",
                "Du kannst mit ihr auch Verstecker fangen.",
                "Wechsle das Item, um wieder zu wachsen."
            )
        )
    )

    @ConfigSerializable
    data class ThemeSound(
        @param:Comment("Sound key, e.g. block.bell.use. Empty = no sound.")
        var key: String = "",
        @param:Comment("master, music, record, weather, block, hostile, neutral, player, ambient, voice, ui")
        var source: String = "master",
        var volume: Float = 1f,
        var pitch: Float = 1f
    )

    @ConfigSerializable
    data class SoundsTheme(
        var gameStart: ThemeSound = ThemeSound("entity.ender_dragon.growl", "hostile", .8f, 1.2f),
        var hidingStarted: ThemeSound = ThemeSound("entity.ender_dragon.growl", "hostile", .5f, .75f),
        var seekersReleased: ThemeSound = ThemeSound("entity.ender_dragon.growl", "hostile", .75f, .75f),
        var gameEnd: ThemeSound = ThemeSound("entity.ender_dragon.death", "hostile", .75f, .75f),
        @param:Comment("Played every second during the last 10 seconds of a countdown.")
        var countdownTick: ThemeSound = ThemeSound("block.note_block.pling", "block", .5f, 2f),
        var roleAssigned: ThemeSound = ThemeSound("entity.player.levelup", "player", .75f, 1f),
        var teleport: ThemeSound = ThemeSound("entity.enderman.teleport", "hostile", .5f, 1f)
    )

    @ConfigSerializable
    data class CelebrationTheme(
        @param:Comment("Launch fireworks at every player while the winners are celebrated.")
        var fireworks: Boolean = false,
        var fireworkColors: MutableList<String> = mutableListOf("#FFFFFF"),
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

        /** Princess and castle theme for the CHARITY ROYALE event. */
        fun charityRoyale() = HideAndSeekTheme(
            prefix = "<$GOLD>♛ <$PINK>Schloss <dark_gray>» ",
            textColor = CREAM,
            valueColor = GOLD,
            roles = RolesTheme(
                hider = RoleTheme("Prinzessin", "Prinzessinnen", "eine", PINK),
                seeker = RoleTheme("Schlosswache", "Schlosswache", "eine", ROYAL_BLUE),
                spectator = RoleTheme("Gast", "Gäste", "ein", LAVENDER),
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
                title = "<$GOLD>♛ <gradient:$PINK:$GOLD><b>Versteckspiel im Schloss</b></gradient> <$GOLD>♛",
                accentColor = GOLD,
                labelColor = CREAM,
                phase = ScoreboardLine("✿", "Phase"),
                time = ScoreboardLine("⌛", "Zeit"),
                role = ScoreboardLine("♛", "Rolle"),
                hiders = ScoreboardLine("❀", "Prinzessinnen"),
                seekers = ScoreboardLine("⚔", "Schlosswache"),
                players = ScoreboardLine("<$LAVENDER>♥", "Gäste"),
                footer = mutableListOf("<gradient:$PINK:$GOLD><b>♥ CHARITY ROYALE – Danke! ♥</b></gradient>")
            ),
            messages = MessagesTheme(
                allInLobby = "Alle Gäste haben sich im Thronsaal versammelt!",
                gameStartChat = mutableListOf(
                    "",
                    "<$GOLD>♛ <gradient:$PINK:$GOLD><b>Hört, hört!</b></gradient> <$GOLD>♛",
                    "",
                    "Das große Versteckspiel im Schloss beginnt gleich!",
                    ""
                ),
                gameStartTitle = "<$GOLD>♛ <gradient:$PINK:$GOLD><b>Hört, hört!</b></gradient> <$GOLD>♛",
                gameStartSubtitle = "<$CREAM>Das Versteckspiel beginnt",
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
                itemUnavailable = "<#E74C3C>Das kannst du gerade nicht benutzen."
            ),
            items = ItemsTheme(
                swordMaterial = "GOLDEN_SWORD",
                sword = ItemTheme(
                    "<$GOLD>Ritterschwert",
                    mutableListOf(
                        "<gray>Ein Hieb, und die Prinzessin",
                        "<gray>ist entdeckt.",
                        "Geschmiedet für die Schlosswache."
                    )
                ),
                bow = ItemTheme(
                    "<$GOLD>Bogen der Schlosswache",
                    mutableListOf("<gray>Halte flüchtende Prinzessinnen", "<gray>aus der Ferne auf.")
                ),
                arrow = ItemTheme("<$GOLD>Königlicher Pfeil", mutableListOf("<gray>Geht niemals aus.")),
                armorColor = ROYAL_BLUE,
                helmet = ItemTheme("<$ROYAL_BLUE>Helm der Schlosswache", mutableListOf("<gray>Im Dienste der Krone.")),
                chestplate = ItemTheme("<$ROYAL_BLUE>Wappenrock der Schlosswache", mutableListOf("<gray>Im Dienste der Krone.")),
                leggings = ItemTheme("<$ROYAL_BLUE>Beinschutz der Schlosswache", mutableListOf("<gray>Im Dienste der Krone.")),
                boots = ItemTheme("<$ROYAL_BLUE>Stiefel der Schlosswache", mutableListOf("<gray>Leise über jeden Marmorboden.")),
                glowMaterial = "GLOWSTONE_DUST",
                glow = ItemTheme(
                    "<$PINK>Feenstaub",
                    mutableListOf("<gray>Lässt alle Prinzessinnen für kurze Zeit leuchten.")
                ),
                shrinkMaterial = "PINK_TULIP",
                shrink = ItemTheme(
                    "<$PINK>Zauberblume",
                    mutableListOf(
                        "<gray>Solange du sie in der Hand hältst,",
                        "<gray>schrumpfst du auf Prinzessinnengröße.",
                        "Auch mit ihr kannst du Prinzessinnen fangen.",
                        "Wechsle das Item, um wieder zu wachsen."
                    )
                )
            ),
            sounds = SoundsTheme(
                gameStart = ThemeSound("block.bell.use", "master", 1f, 1f),
                hidingStarted = ThemeSound("block.bell.resonate", "master", .8f, 1.2f),
                seekersReleased = ThemeSound("event.raid.horn", "master", .6f, 1.4f),
                gameEnd = ThemeSound("ui.toast.challenge_complete", "master", 1f, 1f),
                countdownTick = ThemeSound("block.amethyst_block.chime", "block", 1f, 1.2f),
                roleAssigned = ThemeSound("entity.player.levelup", "player", .75f, 1.2f),
                teleport = ThemeSound("block.amethyst_cluster.break", "player", .8f, 1.2f)
            ),
            celebration = CelebrationTheme(
                fireworks = true,
                fireworkColors = mutableListOf(PINK, GOLD, LAVENDER),
                hearts = true,
                intervalSeconds = 3
            )
        )
    }
}
