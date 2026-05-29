package dev.slne.surf.survival.events.freebuild.command

import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.booleanArgument
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import dev.slne.surf.survival.events.freebuild.permission.PermissionList

fun switchFreebuildSurvivalServerEventsServerNpcCommand() =
    commandTree("switchfreebuildsurvivalservereventssservernpc") {
        withPermission(PermissionList.COMMAND_SWITCH)

        booleanArgument("switch") {
            anyExecutor { sender, arguments ->
                val switch: Boolean by arguments
                val configState = FreebuildPartConfig.getConfig().enableSurvivalEventsNpc

                if (switch == configState) {
                    sender.sendText {
                        appendErrorPrefix()
                        error("Der Survival Events NPC ist bereits ${if (switch) "aktiviert" else "deaktiviert"}.")
                    }
                    return@anyExecutor
                }

                FreebuildPartConfig.edit(true) {
                    this.enableSurvivalEventsNpc = switch
                }

                sender.sendText {
                    appendSuccessPrefix()
                    success("Der Survival Events NPC wurde ")
                    variableValue(if (switch) "aktiviert" else "deaktiviert")
                    success(".")
                }
            }
        }
    }