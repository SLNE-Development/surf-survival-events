package dev.slne.surf.survival.events.hideandseek.command

import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.util.readableString
import dev.slne.surf.survival.events.base.service.GameService
import dev.slne.surf.survival.events.base.util.toGamePosition
import dev.slne.surf.survival.events.hideandseek.config.HideAndSeekConfig
import dev.slne.surf.survival.events.hideandseek.config.SeekerSelection
import dev.slne.surf.survival.events.hideandseek.game.HideAndSeekGame
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekRoleManager
import dev.slne.surf.survival.events.hideandseek.service.HideAndSeekService
import dev.slne.surf.survival.events.hideandseek.util.PermissionRegistry
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.system.measureTimeMillis

fun hideAndSeekCommand() = commandTree("hideandseek") {
    withAliases("has")

    literalArgument("start") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ ->
            if (!GameService.isActiveGame(HideAndSeekGame.KEY)) {
                player.sendText {
                    appendErrorPrefix()
                    error("Hide and Seek ist nicht aktiv. Starte das Event zuerst mit ")
                    variableValue("/survivalevents start hideandseek")
                    error(".")
                }
                return@playerExecutor
            }

            if (HideAndSeekService.phase != HideAndSeekService.Phase.LOBBY) {
                player.sendText {
                    appendErrorPrefix()
                    error("Das Spiel kann gerade nicht gestartet werden.")
                    appendNewline()
                    error("Aktuelle Phase:")
                    appendSpace()
                    variableValue(HideAndSeekService.phase.name)
                }
                return@playerExecutor
            }

            val gameplay = HideAndSeekConfig.getConfig().gameplay
            val joined = GameService.snapshot()?.gamePlayers?.size ?: 0
            if (joined < gameplay.minPlayersToStart) {
                player.sendText {
                    appendErrorPrefix()
                    error("Es sind nicht genug Spieler im Event ")
                    variableValue("($joined/${gameplay.minPlayersToStart})")
                    error(".")
                }
                return@playerExecutor
            }

            if (gameplay.seekerSelection == SeekerSelection.FIXED && gameplay.fixedSeekers.isEmpty()) {
                player.sendText {
                    appendErrorPrefix()
                    error("Der Sucher-Modus steht auf ")
                    variableValue("festgelegt")
                    error(", aber es sind keine Sucher festgelegt. Lege welche mit ")
                    variableValue("/has seeker add")
                    error(" fest oder wechsle mit ")
                    variableValue("/has seeker mode random")
                    error(".")
                }
                return@playerExecutor
            }

            if (joined <= gameplay.seekerAmount) {
                player.sendText {
                    appendErrorPrefix()
                    error("Mit ")
                    variableValue(gameplay.seekerAmount)
                    error(" Suchern und ")
                    variableValue(joined)
                    error(" Spielern bleiben keine Verstecker übrig. Es müssen mehr Spieler beitreten oder weniger Sucher eingestellt werden (")
                    variableValue("/has settings seeker-amount")
                    error(").")
                }
                return@playerExecutor
            }

            val started = GameService.withGameContext(HideAndSeekGame.KEY) {
                HideAndSeekService.beginGame()
            }

            if (started) {
                player.sendText {
                    appendSuccessPrefix()
                    success("Das Spiel wird gestartet...")
                }
            } else {
                player.sendText {
                    appendErrorPrefix()
                    error("Das Spiel wurde bereits gestartet.")
                }
            }
        }
    }

    literalArgument("settings") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)

        intSetting("min-players", min = 1,
            get = { it.gameplay.minPlayersToStart }, set = { c, v -> c.gameplay.minPlayersToStart = v })
        intSetting("seeker-amount", min = 1,
            get = { it.gameplay.seekerAmount }, set = { c, v -> c.gameplay.seekerAmount = v })
        seekerModeSetting()
        boolSetting("hiders-become-seekers",
            get = { it.gameplay.hidersBecomeSeekers }, set = { c, v -> c.gameplay.hidersBecomeSeekers = v })
        boolSetting("one-hit-knockout",
            get = { it.gameplay.oneHitKnockOut }, set = { c, v -> c.gameplay.oneHitKnockOut = v })
        doubleSetting("hider-scale", min = 0.0625, max = 16.0,
            get = { it.gameplay.hiderScale }, set = { c, v -> c.gameplay.hiderScale = v })
        longSetting("special-item-cooldown", min = 0,
            get = { it.gameplay.specialItemCooldownSeconds }, set = { c, v -> c.gameplay.specialItemCooldownSeconds = v })
        longSetting("glow-duration", min = 1,
            get = { it.gameplay.glowEffectDurationSeconds }, set = { c, v -> c.gameplay.glowEffectDurationSeconds = v })

        longSetting("lobby-time", min = 0,
            get = { it.timers.lobbySeconds }, set = { c, v -> c.timers.lobbySeconds = v })
        longSetting("preparation-time", min = 0,
            get = { it.timers.preparationSeconds }, set = { c, v -> c.timers.preparationSeconds = v })
        longSetting("seek-time", min = 1,
            get = { it.timers.seekSeconds }, set = { c, v -> c.timers.seekSeconds = v })
        longSetting("celebration-time", min = 0,
            get = { it.timers.celebrationSeconds }, set = { c, v -> c.timers.celebrationSeconds = v })

        intSetting("start-radius", min = 1,
            get = { it.border.startRadius }, set = { c, v -> c.border.startRadius = v })
        intSetting("end-radius", min = 1,
            get = { it.border.endRadius }, set = { c, v -> c.border.endRadius = v })
        doubleSetting("border-damage", min = 0.0,
            get = { it.border.damage }, set = { c, v -> c.border.damage = v })
        doubleSetting("border-buffer", min = 0.0,
            get = { it.border.buffer }, set = { c, v -> c.border.buffer = v })
    }

    literalArgument("seeker") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        anyExecutor { sender, _ -> sender.sendSeekerInfo() }

        literalArgument("add") {
            entitySelectorArgumentOnePlayer("target") {
                anyExecutor { sender, args ->
                    val target: Player by args
                    addFixedSeeker(sender, target)
                }
            }
        }

        literalArgument("remove") {
            textArgument("name") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { fixedSeekerNames() })
                anyExecutor { sender, args ->
                    val name: String by args
                    removeFixedSeeker(sender, name)
                }
            }
        }
    }

    literalArgument("bypass") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ ->
            val bypassing = HideAndSeekRoleManager.switchBypass(player)

            player.sendText {
                appendSuccessPrefix()
                if (bypassing) {
                    success("Du umgehst nun die Event-Einschränkungen.")
                } else {
                    success("Du umgehst die Event-Einschränkungen nicht mehr.")
                }
            }
        }
    }

    literalArgument("reload") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        anyExecutor { sender, _ ->
            val ms = measureTimeMillis {
                HideAndSeekConfig.reloadFromFile()
            }

            sender.sendText {
                appendSuccessPrefix()
                success("Die Hide-and-Seek-Config wurde erfolgreich neu geladen ")
                spacer("(${ms}ms)")
            }
        }
    }

    literalArgument("set-lobby") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ ->
            val location = player.location

            HideAndSeekConfig.edit {
                lobbySpawn = location.toGamePosition()
            }
            HideAndSeekConfig.save()

            player.sendText {
                appendSuccessPrefix()
                success("Der Lobby-Spawn wurde erfolgreich gesetzt!")
                appendSpace()
                variableValue(location.readableString(true))
            }
        }
    }

    literalArgument("set-game-spawn") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ ->
            val location = player.location

            HideAndSeekConfig.edit {
                gameSpawn = location.toGamePosition()
            }
            HideAndSeekConfig.save()

            player.sendText {
                appendSuccessPrefix()
                success("Der Spiel-Spawn wurde erfolgreich gesetzt!")
                appendSpace()
                variableValue(location.readableString(true))
            }
        }
    }

    literalArgument("set-spectator") {
        withPermission(PermissionRegistry.COMMAND_COMMUNITY_MANAGER)
        playerExecutor { player, _ ->
            val location = player.location

            HideAndSeekConfig.edit {
                spectatorSpawn = location.toGamePosition()
            }
            HideAndSeekConfig.save()

            player.sendText {
                appendSuccessPrefix()
                success("Der Spectator-Spawn wurde erfolgreich gesetzt!")
                appendSpace()
                variableValue(location.readableString(true))
            }
        }
    }
}

private fun fixedSeekerNames(): List<String> {
    return HideAndSeekConfig.getConfig().gameplay.fixedSeekers.mapNotNull { uuid ->
        runCatching { Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name }.getOrNull()
    }
}

private fun Argument<*>.seekerModeSetting() = literalArgument("seeker-mode") {
    anyExecutor { sender, _ ->
        sender.querySetting("seeker-mode", seekerModeName(HideAndSeekConfig.getConfig().gameplay.seekerSelection))
    }
    literalArgument("random") {
        anyExecutor { sender, _ -> sender.setSeekerMode(SeekerSelection.RANDOM) }
    }
    literalArgument("fixed") {
        anyExecutor { sender, _ -> sender.setSeekerMode(SeekerSelection.FIXED) }
    }
}

private fun seekerModeName(mode: SeekerSelection) =
    if (mode == SeekerSelection.FIXED) "festgelegt" else "zufällig"

private fun CommandSender.setSeekerMode(mode: SeekerSelection) {
    HideAndSeekConfig.edit { gameplay.seekerSelection = mode }
    announceSetting("seeker-mode", seekerModeName(mode))
}

private fun addFixedSeeker(sender: CommandSender, target: Player) {
    val gameplay = HideAndSeekConfig.getConfig().gameplay
    val uuid = target.uniqueId.toString()

    if (uuid in gameplay.fixedSeekers) {
        sender.sendText {
            appendErrorPrefix()
            variableValue(target.name)
            error(" ist bereits als Sucher festgelegt.")
        }
        return
    }

    if (gameplay.fixedSeekers.size >= gameplay.seekerAmount) {
        sender.sendText {
            appendErrorPrefix()
            error("Es sind bereits ")
            variableValue(gameplay.seekerAmount)
            error(" Sucher festgelegt. Entferne zuerst einen mit ")
            variableValue("/has seeker remove")
            error(".")
        }
        return
    }

    HideAndSeekConfig.edit { this.gameplay.fixedSeekers.add(uuid) }
    HideAndSeekConfig.save()

    val current = HideAndSeekConfig.getConfig().gameplay
    sender.sendText {
        appendSuccessPrefix()
        variableValue(target.name)
        success(" wurde als Sucher festgelegt ")
        variableValue("(${current.fixedSeekers.size}/${current.seekerAmount})")
        success(".")
    }
}

private fun removeFixedSeeker(sender: CommandSender, name: String) {
    val match = HideAndSeekConfig.getConfig().gameplay.fixedSeekers.firstOrNull { uuid ->
        runCatching { Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name }.getOrNull()
            ?.equals(name, ignoreCase = true) == true
    }

    if (match == null) {
        sender.sendText {
            appendErrorPrefix()
            variableValue(name)
            error(" ist nicht als Sucher festgelegt.")
        }
        return
    }

    HideAndSeekConfig.edit { gameplay.fixedSeekers.remove(match) }
    HideAndSeekConfig.save()

    sender.sendText {
        appendSuccessPrefix()
        variableValue(name)
        success(" ist kein festgelegter Sucher mehr.")
    }
}

private fun CommandSender.sendSeekerInfo() {
    val gameplay = HideAndSeekConfig.getConfig().gameplay
    val names = fixedSeekerNames()

    sendText {
        appendInfoPrefix()
        info("Sucher-Auswahl: ")
        variableValue(seekerModeName(gameplay.seekerSelection))
        appendNewline()
        info("Festgelegte Sucher ")
        variableValue("(${gameplay.fixedSeekers.size}/${gameplay.seekerAmount})")
        info(": ")
        if (names.isEmpty()) {
            spacer("keine")
        } else {
            variableValue(names.joinToString(", "))
        }
    }
}

private fun Argument<*>.intSetting(
    name: String,
    min: Int = Int.MIN_VALUE,
    get: (HideAndSeekConfig) -> Int,
    set: (HideAndSeekConfig, Int) -> Unit,
) = literalArgument(name) {
    anyExecutor { sender, _ -> sender.querySetting(name, get(HideAndSeekConfig.getConfig())) }
    integerArgument("value", min) {
        anyExecutor { sender, args ->
            val value: Int by args
            HideAndSeekConfig.edit { set(this, value) }
            sender.announceSetting(name, value)
        }
    }
}

private fun Argument<*>.longSetting(
    name: String,
    min: Long = Long.MIN_VALUE,
    get: (HideAndSeekConfig) -> Long,
    set: (HideAndSeekConfig, Long) -> Unit,
) = literalArgument(name) {
    anyExecutor { sender, _ -> sender.querySetting(name, get(HideAndSeekConfig.getConfig())) }
    longArgument("value", min) {
        anyExecutor { sender, args ->
            val value: Long by args
            HideAndSeekConfig.edit { set(this, value) }
            sender.announceSetting(name, value)
        }
    }
}

private fun Argument<*>.doubleSetting(
    name: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    get: (HideAndSeekConfig) -> Double,
    set: (HideAndSeekConfig, Double) -> Unit,
) = literalArgument(name) {
    anyExecutor { sender, _ -> sender.querySetting(name, get(HideAndSeekConfig.getConfig())) }
    doubleArgument("value", min, max) {
        anyExecutor { sender, args ->
            val value: Double by args
            HideAndSeekConfig.edit { set(this, value) }
            sender.announceSetting(name, value)
        }
    }
}

private fun Argument<*>.boolSetting(
    name: String,
    get: (HideAndSeekConfig) -> Boolean,
    set: (HideAndSeekConfig, Boolean) -> Unit,
) = literalArgument(name) {
    anyExecutor { sender, _ -> sender.querySetting(name, get(HideAndSeekConfig.getConfig())) }
    booleanArgument("value") {
        anyExecutor { sender, args ->
            val value: Boolean by args
            HideAndSeekConfig.edit { set(this, value) }
            sender.announceSetting(name, value)
        }
    }
}

private fun CommandSender.querySetting(name: String, value: Any) {
    sendText {
        appendInfoPrefix()
        info("Die Einstellung ")
        variableValue(name)
        info(" steht auf ")
        variableValue(value.toString())
        info(".")
    }
}

private fun CommandSender.announceSetting(name: String, value: Any) {
    sendText {
        appendSuccessPrefix()
        success("Die Einstellung ")
        variableValue(name)
        success(" wurde auf ")
        variableValue(value.toString())
        success(" gesetzt.")
    }
}
