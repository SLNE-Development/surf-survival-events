package dev.slne.surf.survival.events.red.light.green.light.command.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.doubleArgument
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.integerArgument
import dev.jorel.commandapi.kotlindsl.longArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.red.light.green.light.config.RlglConfig
import dev.slne.surf.survival.events.red.light.green.light.service.RlglService
import dev.slne.surf.survival.events.red.light.green.light.utils.PermissionList
import org.bukkit.command.CommandSender

fun CommandAPICommand.debugCommand() = subcommand("debug") {
    withPermission(PermissionList.COMMAND_COMMUNITY_MANAGER)

    subcommand("reload") {
        anyExecutor { sender, _ ->
            RlglConfig.reloadFromFile()

            sender.sendText {
                appendSuccessPrefix()
                success("Die Red Light, Green Light Konfiguration wurde neu geladen.")
            }
        }
    }

    subcommand("validateConfig") {
        anyExecutor { sender, _ ->
            try {
                RlglService.validateConfig()

                sender.sendText {
                    appendSuccessPrefix()
                    success("Die Konfiguration ist vollständig und gültig.")
                }
            } catch (e: IllegalStateException) {
                sender.sendText {
                    appendErrorPrefix()
                    error(e.message ?: "Die Konfiguration ist ungültig.")
                }
            }
        }
    }

    subcommand("set") {
        subcommand("minGreenLightSeconds") {
            integerArgument("value", min = 1)
            anyExecutor { sender, args ->
                val value: Int by args
                RlglConfig.edit { gameplay.minGreenLightSeconds = value }
                reportConfigSet(sender, "minGreenLightSeconds", value.toString())
            }
        }

        subcommand("maxGreenLightSeconds") {
            integerArgument("value", min = 1)
            anyExecutor { sender, args ->
                val value: Int by args
                RlglConfig.edit { gameplay.maxGreenLightSeconds = value }
                reportConfigSet(sender, "maxGreenLightSeconds", value.toString())
            }
        }

        subcommand("minRedLightSeconds") {
            integerArgument("value", min = 1)
            anyExecutor { sender, args ->
                val value: Int by args
                RlglConfig.edit { gameplay.minRedLightSeconds = value }
                reportConfigSet(sender, "minRedLightSeconds", value.toString())
            }
        }

        subcommand("maxRedLightSeconds") {
            integerArgument("value", min = 1)
            anyExecutor { sender, args ->
                val value: Int by args
                RlglConfig.edit { gameplay.maxRedLightSeconds = value }
                reportConfigSet(sender, "maxRedLightSeconds", value.toString())
            }
        }

        subcommand("redLightGraceMillis") {
            longArgument("value", min = 0)
            anyExecutor { sender, args ->
                val value: Long by args
                RlglConfig.edit { gameplay.redLightGraceMillis = value }
                reportConfigSet(sender, "redLightGraceMillis", value.toString())
            }
        }

        subcommand("inviteRadius") {
            doubleArgument("value", min = 0.0)
            anyExecutor { sender, args ->
                val value: Double by args
                RlglConfig.edit { gameplay.inviteRadius = value }
                reportConfigSet(sender, "inviteRadius", value.toString())
            }
        }

        subcommand("startCountdownSeconds") {
            integerArgument("value", min = 0)
            anyExecutor { sender, args ->
                val value: Int by args
                RlglConfig.edit { gameplay.startCountdownSeconds = value }
                reportConfigSet(sender, "startCountdownSeconds", value.toString())
            }
        }
    }
}

private fun reportConfigSet(sender: CommandSender, key: String, value: String) {
    sender.sendText {
        appendSuccessPrefix()
        variableValue(key)
        appendSpace()
        success("wurde auf")
        appendSpace()
        variableValue(value)
        appendSpace()
        success("gesetzt.")
    }
}