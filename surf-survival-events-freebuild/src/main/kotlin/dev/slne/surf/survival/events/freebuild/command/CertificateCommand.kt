package dev.slne.surf.survival.events.freebuild.command

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.kotlindsl.commandTree
import dev.jorel.commandapi.kotlindsl.getValue
import dev.jorel.commandapi.kotlindsl.greedyStringArgument
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.slne.surf.api.core.command.args.awaitingOrNull
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.core.api.common.player.SurfPlayer
import dev.slne.surf.core.api.paper.command.argument.surfOfflinePlayerArgument
import dev.slne.surf.survival.events.freebuild.certificate.CertificateCopies
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDates
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDelivery
import dev.slne.surf.survival.events.freebuild.certificate.CertificateDialogs
import dev.slne.surf.survival.events.freebuild.certificate.CertificateForm
import dev.slne.surf.survival.events.freebuild.certificate.CertificateRecord
import dev.slne.surf.survival.events.freebuild.certificate.CertificateService
import dev.slne.surf.survival.events.freebuild.certificate.CertificateSuggestions
import dev.slne.surf.survival.events.freebuild.certificate.GiveResult
import dev.slne.surf.survival.events.freebuild.permission.PermissionList
import dev.slne.surf.survival.events.freebuild.plugin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.LocalDate
import java.util.logging.Level

fun certificateCommand() = commandTree("certificate") {
    withPermission(PermissionList.COMMAND_CERTIFICATE)

    playerExecutor { player, _ ->
        CertificateDialogs.openForm(player)
    }

    literalArgument("give") {
        surfOfflinePlayerArgument("player") {
            stringArgument("date") {
                replaceSuggestions(ArgumentSuggestions.stringCollection<CommandSender> {
                    CertificateSuggestions.dates(CertificateService.allCertificates(), LocalDate.now())
                })

                greedyStringArgument("eventName") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection<CommandSender> {
                        CertificateSuggestions.eventNames(CertificateService.allCertificates())
                    })

                    playerExecutorSuspend { player, args ->
                        val date: String by args
                        val eventName: String by args
                        give(player, args, date, eventName)
                    }
                }
            }
        }
    }

    literalArgument("copy") {
        withPermission(PermissionList.COMMAND_CERTIFICATE_COPY)

        surfOfflinePlayerArgument("player") {
            playerExecutorSuspend { player, args ->
                copy(player, args, eventName = null)
            }

            greedyStringArgument("eventName") {
                replaceSuggestions(ArgumentSuggestions.stringCollection<CommandSender> { info ->
                    val playerName = info.previousArgs().getRaw("player").orEmpty()
                    CertificateSuggestions.eventNamesOf(CertificateService.allCertificates(), playerName)
                })

                playerExecutorSuspend { player, args ->
                    val eventName: String by args
                    copy(player, args, eventName)
                }
            }
        }
    }
}

private suspend fun give(player: Player, args: CommandArguments, date: String, eventName: String) {
    val target = args.awaitingOrNull<SurfPlayer?>("player")
    val targetName = target?.lastKnownName

    if (target == null || targetName == null) {
        player.sendText {
            appendErrorPrefix()
            error("Der Spieler wurde nicht gefunden.")
        }
        return
    }

    val parsedDate = CertificateDates.parse(date)
    if (parsedDate == null) {
        player.sendText {
            appendErrorPrefix()
            error(CertificateForm.INVALID_DATE)
        }
        return
    }

    val event = CertificateForm.normalizeEventName(eventName)
    val eventNameError = CertificateForm.eventNameError(event)
    if (eventNameError != null) {
        player.sendText {
            appendErrorPrefix()
            error(eventNameError)
        }
        return
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
        return
    }

    when (result) {
        is GiveResult.Issued -> {
            val record = result.certificate.record
            CertificateDelivery.notifyIfOnline(record)
            player.sendText {
                appendSuccessPrefix()
                success("Die Teilnahmeurkunde für ")
                variableValue(targetName)
                success(" wurde erstellt und kann jetzt abgeholt werden.")
                appendCopyButton(player, record)
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
                appendCopyButton(player, result.record)
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

/**
 * Puts a copy of a certificate of the given player into the inventory of [player]. Without [eventName], or if the
 * player has several certificates for it, they are listed with a button each instead.
 */
private suspend fun copy(player: Player, args: CommandArguments, eventName: String?) {
    val target = args.awaitingOrNull<SurfPlayer?>("player")
    if (target == null) {
        player.sendText {
            appendErrorPrefix()
            error("Der Spieler wurde nicht gefunden.")
        }
        return
    }

    val certificates = CertificateService.certificatesOf(target.uuid)
    val ownerName = target.lastKnownName ?: certificates.firstOrNull()?.playerName ?: args.getRaw("player").orEmpty()
    val matches = if (eventName == null) certificates else CertificateSuggestions.forEvent(certificates, eventName)

    when {
        certificates.isEmpty() -> player.sendText {
            appendErrorPrefix()
            variableValue(ownerName)
            error(" hat noch keine Teilnahmeurkunden.")
        }

        matches.isEmpty() -> player.sendText {
            appendErrorPrefix()
            variableValue(ownerName)
            error(" hat keine Teilnahmeurkunde für ")
            variableValue(CertificateForm.normalizeEventName(eventName))
            error(".")
        }

        matches.size == 1 -> withContext(plugin.entityDispatcher(player)) {
            CertificateCopies.give(player, matches.single())
        }

        else -> CertificateCopies.sendList(player, ownerName, matches)
    }
}

private fun SurfComponentBuilder.appendCopyButton(
    player: Player,
    record: CertificateRecord,
) {
    if (player.hasPermission(PermissionList.COMMAND_CERTIFICATE_COPY)) {
        append(Component.space())
        append(CertificateCopies.button(record.mapId))
    }
}
