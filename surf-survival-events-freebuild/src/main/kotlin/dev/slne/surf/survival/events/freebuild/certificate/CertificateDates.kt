package dev.slne.surf.survival.events.freebuild.certificate

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object CertificateDates {
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    fun parse(input: String): LocalDate? = try {
        LocalDate.parse(input, formatter)
    } catch (_: DateTimeParseException) {
        null
    }

    fun format(date: LocalDate): String = formatter.format(date)
}
