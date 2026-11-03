package dev.slne.surf.survival.events.freebuild.certificate

object TextWrap {
    private const val ELLIPSIS = "..."
    private val whitespace = Regex("\\s+")

    /**
     * Greedily wraps [text] at word boundaries into at most [maxLines] lines no wider than [maxWidth].
     * Whatever does not fit into the last line is cut off and marked with an ellipsis.
     */
    fun wrap(text: String, maxWidth: Int, maxLines: Int, measure: (String) -> Int): List<String> {
        val words = text.trim().split(whitespace).filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        var index = 0

        while (index < words.size && lines.size < maxLines) {
            var line = words[index++]
            while (index < words.size && measure("$line ${words[index]}") <= maxWidth) {
                line += " ${words[index++]}"
            }

            val isLastLine = lines.size == maxLines - 1
            if (isLastLine && index < words.size) {
                line += words.subList(index, words.size).joinToString(" ", prefix = " ")
                index = words.size
            }

            lines += if (measure(line) <= maxWidth) line else truncate(line, maxWidth, measure)
        }

        return lines
    }

    private fun truncate(line: String, maxWidth: Int, measure: (String) -> Int): String {
        var end = line.length
        while (end > 0 && measure(line.take(end).trimEnd() + ELLIPSIS) > maxWidth) end--
        return line.take(end).trimEnd() + ELLIPSIS
    }
}
