package dev.slne.surf.survival.events.freebuild.certificate

import kotlin.math.roundToInt

/** The values entered in the certificate dialog. [date] stays text, so invalid input can be shown again. */
data class CertificateForm(
    val eventName: String,
    val date: String,
    val server: String,
    val minimumMinutes: Int,
) {
    /** Returns an error message for the first invalid value, or `null` if the form is complete. */
    fun validate(): String? = eventNameError(eventName) ?: when {
        CertificateDates.parse(date) == null -> INVALID_DATE
        server.isEmpty() -> "Bitte gib den Server an, auf dem das Event stattgefunden hat."
        else -> null
    }

    companion object {
        const val MAX_EVENT_NAME_LENGTH = 64
        const val INVALID_DATE = "Ungültiges Datum. Bitte nutze das Format TT.MM.JJJJ, z. B. 02.11.2026."

        private val whitespace = Regex("\\s+")

        fun from(eventName: String?, date: String?, server: String?, minimumMinutes: Float?) = CertificateForm(
            eventName = normalizeEventName(eventName),
            date = date.orEmpty().trim(),
            server = server.orEmpty().trim(),
            minimumMinutes = minimumMinutes?.takeIf { it.isFinite() }?.roundToInt()?.coerceAtLeast(0) ?: 0,
        )

        fun normalizeEventName(eventName: String?) = eventName.orEmpty().trim().replace(whitespace, " ")

        fun eventNameError(eventName: String): String? {
            if (eventName.isEmpty() || eventName.length > MAX_EVENT_NAME_LENGTH) {
                return "Der Eventname muss zwischen 1 und $MAX_EVENT_NAME_LENGTH Zeichen lang sein."
            }

            val undrawable = PixelFont.undrawable(eventName)
            if (undrawable.isNotEmpty()) {
                return "Diese Zeichen können auf der Urkunde nicht dargestellt werden: ${undrawable.joinToString(" ")}"
            }

            return null
        }
    }
}
