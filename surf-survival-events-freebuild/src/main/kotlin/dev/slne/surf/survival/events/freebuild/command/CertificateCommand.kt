package dev.slne.surf.survival.events.freebuild.command

import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.command.args.awaitingOrNull
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.core.api.common.player.SurfPlayer
import dev.slne.surf.core.api.paper.command.argument.surfOfflinePlayerArgument
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDates
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDelivery
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDialogs
import dev.slne.surf.survival.events.freebuild.certificate.CertificateForm
import dev.slne.surf.survival.events.freebuild.certificate.CertificateService
import dev.slne.surf.survival.events.freebuild.certificate.GiveResult
import dev.slne.surf.survival.events.freebuild.permission.PermissionList
import dev.slne.surf.survival.events.freebuild.plugin
import kotlinx.coroutines.CancellationException
import java.util.logging.Level

fun certificateCommand() = commandTree("certificate") {
    withPermission(PermissionList.COMMAND_CERTIFICATE)

    playerExecutor { player, _ ->
        CertificateDialogs.openForm(player)
    }

    literalArgument("give") {
        surfOfflinePlayerArgument("player") {
            stringArgument("date") {
                greedyStringArgument("eventName") {
                    playerExecutorSuspend { player, args ->
                        val date: String by args
                        val eventName: String by args
                        val target = args.awaitingOrNull<SurfPlayer?>("player")
                        val targetName = target?.lastKnownName

                        if (target == null || targetName == null) {
                            player.sendText {
                                appendErrorPrefix()
                                error("Der Spieler wurde nicht gefunden.")
                            }
                            return@playerExecutorSuspend
                        }

                        val parsedDate = CertificateDates.parse(date)
                        if (parsedDate == null) {
                            player.sendText {
                                appendErrorPrefix()
                                error(CertificateForm.INVALID_DATE)
                            }
                            return@playerExecutorSuspend
                        }

                        val event = CertificateForm.normalizeEventName(eventName)
                        val eventNameError = CertificateForm.eventNameError(event)
                        if (eventNameError != null) {
                            player.sendText {
                                appendErrorPrefix()
                                error(eventNameError)
                            }
                            return@playerExecutorSuspend
                        }

                        val result = try {
                            CertificateService.give(target.uuid, targetName, event, parsedDate, player.uniqueId) {
                                player.sendText {
                                    appendInfoPrefix()
                                    info("Die Teilnahmeurkunde für ")
                                    variableValue(targetName)
                                    info(" wird erstellt...")
                                }
                            }
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            plugin.logger.log(Level.SEVERE, "Failed to give a certificate to $targetName", exception)
                            player.sendText {
                                appendErrorPrefix()
                                error("Beim Vergeben der Teilnahmeurkunde ist ein Fehler aufgetreten.")
                            }
                            return@playerExecutorSuspend
                        }

                        when (result) {
                            is GiveResult.Issued -> {
                                CertificateDelivery.notifyIfOnline(result.certificate.record)
                                player.sendText {
                                    appendSuccessPrefix()
                                    success("Die Teilnahmeurkunde für ")
                                    variableValue(targetName)
                                    success(" wurde erstellt und kann jetzt abgeholt werden.")
                                    if (result.certificate.skinUnavailable) {
                                        appendNewline()
                                        error("Der Skin konnte nicht geladen werden, die Urkunde zeigt den Standardkopf.")
                                    }
                                }
                            }

                            is GiveResult.Offered -> {
                                CertificateDelivery.notifyIfOnline(result.record)
                                player.sendText {
                                    appendSuccessPrefix()
                                    variableValue(targetName)
                                    if (result.wasClaimed) {
                                        success(" hatte die Teilnahmeurkunde bereits abgeholt, ")
                                        success("sie kann jetzt erneut abgeholt werden.")
                                    } else {
                                        success(" hat die Teilnahmeurkunde bereits und kann sie noch abholen.")
                                    }
                                }
                            }

                            GiveResult.InProgress -> player.sendText {
                                appendErrorPrefix()
                                error("Die Teilnahmeurkunde für ")
                                variableValue(targetName)
                                error(" wird gerade erstellt.")
                            }
                        }
                    }
                }
            }
        }
    }
}
