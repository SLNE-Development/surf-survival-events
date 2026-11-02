package dev.slne.surf.survival.events.freebuild.command

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.command.args.awaitingOrNull
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.core.api.common.player.SurfPlayer
import dev.slne.surf.core.api.paper.command.argument.surfOfflinePlayerArgument
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDates
import dev.slne.surf.survival.events.freebuild.certificate.CertificateService
import dev.slne.surf.survival.events.freebuild.permission.PermissionList
import dev.slne.surf.survival.events.freebuild.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.util.logging.Level

private const val MAX_EVENT_NAME_LENGTH = 64

fun certificateCommand() = commandTree("certificate") {
    withPermission(PermissionList.COMMAND_CERTIFICATE)

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
                            error("Ungültiges Datum. Bitte nutze das Format TT.MM.JJJJ, z. B. 02.11.2026.")
                        }
                        return@playerExecutorSuspend
                    }

                    val event = eventName.trim().replace(Regex("\\s+"), " ")
                    if (event.isEmpty() || event.length > MAX_EVENT_NAME_LENGTH) {
                        player.sendText {
                            appendErrorPrefix()
                            error("Der Eventname muss zwischen 1 und $MAX_EVENT_NAME_LENGTH Zeichen lang sein.")
                        }
                        return@playerExecutorSuspend
                    }

                    player.sendText {
                        appendInfoPrefix()
                        info("Die Teilnahmeurkunde für ")
                        variableValue(targetName)
                        info(" wird erstellt...")
                    }

                    val certificate = try {
                        CertificateService.issue(target.uuid, targetName, event, parsedDate, player.uniqueId)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        plugin.logger.log(Level.SEVERE, "Failed to create a certificate for $targetName", exception)
                        player.sendText {
                            appendErrorPrefix()
                            error("Beim Erstellen der Teilnahmeurkunde ist ein Fehler aufgetreten.")
                        }
                        return@playerExecutorSuspend
                    }

                    withContext(plugin.entityDispatcher(player)) {
                        if (!player.isConnected) {
                            plugin.logger.warning(
                                "${player.name} left before receiving the certificate for $targetName " +
                                        "(map #${certificate.record.mapId}); it is stored but was not handed out"
                            )
                            return@withContext
                        }

                        val result = player.give(certificate.item)
                        player.sendText {
                            appendSuccessPrefix()
                            success("Du hast die Teilnahmeurkunde für ")
                            variableValue(targetName)
                            success(" erhalten.")
                            if (result.drops().isNotEmpty()) {
                                success(" Dein Inventar war voll, sie liegt auf dem Boden.")
                            }
                        }
                    }
                }
            }
        }
    }
}
