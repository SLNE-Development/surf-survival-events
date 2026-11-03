@file:Suppress("UnstableApiUsage")

package dev.slne.surf.survival.events.freebuild.certificate

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.dialog.base
import dev.slne.surf.api.paper.dialog.dialog
import dev.slne.surf.api.paper.dialog.type
import dev.slne.surf.core.api.common.SurfCoreApi
import dev.slne.surf.survival.events.freebuild.config.FreebuildPartConfig
import dev.slne.surf.survival.events.freebuild.plugin
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.dialog.DialogResponseView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import java.time.LocalDate
import java.util.logging.Level

/**
 * The dialogs behind `/certificate`: a form for event, date, server and minimum playtime, followed by a preview of
 * all players that would get a certificate.
 */
object CertificateDialogs {
    private const val DEFAULT_MINIMUM_MINUTES = 10
    private const val MAX_MINIMUM_MINUTES = 120
    private const val MINIMUM_MINUTES_STEP = 5
    private const val PARALLEL_NAME_LOOKUPS = 8
    private const val BODY_WIDTH = 300

    fun openForm(player: Player, form: CertificateForm = defaultForm(), error: String? = null) {
        player.showDialog(formDialog(form, error))
    }

    private fun defaultForm(): CertificateForm {
        val config = FreebuildPartConfig.getConfig()
        val server = SurfCoreApi.getServerByName(config.eventServerName)?.displayName
            ?: config.eventServerDisplayName.ifBlank { config.eventServerName }

        return CertificateForm(
            eventName = "",
            date = CertificateDates.format(LocalDate.now()),
            server = server,
            minimumMinutes = DEFAULT_MINIMUM_MINUTES
        )
    }

    private fun formDialog(form: CertificateForm, error: String?): Dialog = dialog {
        base {
            title { info("Teilnahmeurkunden vergeben") }
            body {
                if (error != null) {
                    plainMessage(BODY_WIDTH) { error(error) }
                }
                plainMessage(BODY_WIDTH) {
                    info("Alle Spieler, die am Eventtag mindestens so lange auf dem Server waren, ")
                    info("bekommen eine Teilnahmeurkunde.")
                }
            }
            input {
                text("event") {
                    label { info("Eventname") }
                    initial(form.eventName)
                    maxLength(CertificateForm.MAX_EVENT_NAME_LENGTH)
                }
                text("date") {
                    label { info("Datum (TT.MM.JJJJ)") }
                    initial(form.date)
                    maxLength(10)
                }
                text("server") {
                    label { info("Server") }
                    initial(form.server)
                    maxLength(64)
                }
                numberRange("minutes", 0..MAX_MINIMUM_MINUTES) {
                    label { info("Mindestspielzeit") }
                    initial(form.minimumMinutes.coerceIn(0, MAX_MINIMUM_MINUTES).toFloat())
                    step(MINIMUM_MINUTES_STEP.toFloat())
                    labelFormat("%s: %s Minuten")
                }
            }
        }
        type {
            confirmation {
                yes {
                    label { text("Teilnehmer suchen") }
                    action {
                        customPlayerClick { response, player -> search(player, response) }
                    }
                }
                no {
                    label { text("Abbrechen") }
                }
            }
        }
    }

    private fun search(player: Player, response: DialogResponseView) {
        val form = CertificateForm.from(
            eventName = response.getText("event"),
            date = response.getText("date"),
            server = response.getText("server"),
            minimumMinutes = response.getFloat("minutes")
        )

        val error = form.validate()
        if (error != null) {
            openForm(player, form, error)
            return
        }

        val date = checkNotNull(CertificateDates.parse(form.date))
        player.sendText {
            appendInfoPrefix()
            info("Die Teilnehmer werden gesucht...")
        }

        plugin.launch(Dispatchers.IO) {
            try {
                val playtime = CertificatePlaytime.playtimeOn(form.server, date)
                if (playtime == null) {
                    player.sendText {
                        appendErrorPrefix()
                        error("Die Spielzeiten konnten nicht geladen werden, surf-playtime fehlt oder ist zu alt.")
                    }
                    return@launch
                }

                val selection = Participants.select(playtime, form.minimumMinutes) {
                    CertificateService.hasCertificate(it, form.eventName, date)
                }
                val participants = resolveNames(selection.eligible)
                val preview = previewDialog(
                    form = form,
                    date = date,
                    participants = participants,
                    alreadyCertified = selection.alreadyCertified.size,
                    unknownNames = selection.eligible.size - participants.size
                )

                withContext(plugin.entityDispatcher(player)) {
                    if (player.isConnected) {
                        player.showDialog(preview)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to search certificate participants", exception)
                player.sendText {
                    appendErrorPrefix()
                    error("Beim Suchen der Teilnehmer ist ein Fehler aufgetreten.")
                }
            }
        }
    }

    private suspend fun resolveNames(participants: List<Participant>): List<NamedParticipant> = coroutineScope {
        val semaphore = Semaphore(PARALLEL_NAME_LOOKUPS)

        participants.map { participant ->
            async {
                semaphore.withPermit {
                    val name = try {
                        SurfCoreApi.getOfflinePlayer(participant.uuid)?.lastKnownName
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        plugin.logger.log(Level.WARNING, "Failed to look up the name of ${participant.uuid}", exception)
                        null
                    }

                    name?.let { NamedParticipant(participant.uuid, it, participant.seconds) }
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun previewDialog(
        form: CertificateForm,
        date: LocalDate,
        participants: List<NamedParticipant>,
        alreadyCertified: Int,
        unknownNames: Int
    ): Dialog = dialog {
        base {
            title { info("Teilnahmeurkunden vergeben") }
            body {
                plainMessage(BODY_WIDTH) {
                    info("Event: ")
                    variableValue(form.eventName)
                    appendNewline()
                    info("Datum: ")
                    variableValue(form.date)
                    appendNewline()
                    info("Server: ")
                    variableValue(form.server)
                    appendNewline()
                    info("Mindestspielzeit: ")
                    variableValue("${form.minimumMinutes} Minuten")
                }
                plainMessage(BODY_WIDTH) {
                    if (participants.isEmpty()) {
                        info("Es wurden keine Spieler gefunden, die eine Teilnahmeurkunde bekommen.")
                    } else {
                        variableValue("${participants.size} Spieler")
                        info(" bekommen eine Teilnahmeurkunde:")
                        appendNewline()
                        participants.forEachIndexed { index, participant ->
                            if (index > 0) {
                                spacer(", ")
                            }
                            variableValue(participant.name)
                            spacer(" (${participant.seconds / 60} min)")
                        }
                    }
                }
                if (alreadyCertified > 0) {
                    plainMessage(BODY_WIDTH) {
                        variableValue("$alreadyCertified Spieler")
                        info(" haben bereits eine Teilnahmeurkunde für dieses Event und werden übersprungen.")
                    }
                }
                if (unknownNames > 0) {
                    plainMessage(BODY_WIDTH) {
                        error("Von $unknownNames Spielern ist kein Name bekannt, sie werden übersprungen.")
                    }
                }
            }
        }
        type {
            if (participants.isEmpty()) {
                notice {
                    label { text("Zurück") }
                    action {
                        playerCallback { openForm(it, form) }
                    }
                }
            } else {
                confirmation {
                    yes {
                        label { text("Urkunden vergeben") }
                        action {
                            customPlayerClick(ClickCallback.Options.builder().uses(1).build()) { _, player ->
                                issue(player, form, date, participants)
                            }
                        }
                    }
                    no {
                        label { text("Zurück") }
                        action {
                            playerCallback { openForm(it, form) }
                        }
                    }
                }
            }
        }
    }

    private fun issue(player: Player, form: CertificateForm, date: LocalDate, participants: List<NamedParticipant>) {
        player.sendText {
            appendInfoPrefix()
            info("Die Teilnahmeurkunden für ")
            variableValue("${participants.size} Spieler")
            info(" werden erstellt, das kann einen Moment dauern...")
        }

        plugin.launch(Dispatchers.IO) {
            val result = try {
                CertificateService.issueAll(
                    participants = participants,
                    eventName = form.eventName,
                    date = date,
                    createdBy = player.uniqueId,
                    onIssued = CertificateDelivery::notifyIfOnline
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                plugin.logger.log(Level.SEVERE, "Failed to issue certificates for ${form.eventName}", exception)
                player.sendText {
                    appendErrorPrefix()
                    error("Beim Erstellen der Teilnahmeurkunden ist ein Fehler aufgetreten.")
                }
                return@launch
            }

            player.sendText {
                appendSuccessPrefix()
                variableValue("${result.issued.size} Teilnahmeurkunden")
                success(" für ")
                variableValue(form.eventName)
                success(" wurden erstellt und können jetzt abgeholt werden.")
                if (result.skipped.isNotEmpty()) {
                    appendNewline()
                    info("Übersprungen, da bereits vorhanden: ")
                    variableValue(result.skipped.joinToString { it.name })
                }
                if (result.withoutSkin.isNotEmpty()) {
                    appendNewline()
                    error("Mit Standardkopf, da der Skin nicht geladen werden konnte: ")
                    variableValue(result.withoutSkin.joinToString { it.name })
                }
                if (result.failed.isNotEmpty()) {
                    appendNewline()
                    error("Fehlgeschlagen: ")
                    variableValue(result.failed.joinToString { it.name })
                }
            }
        }
    }
}
