package java.time.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/** Formatador só com os padrões usados pela interface ("dd/MM/yyyy" e "EEEE, d 'de' MMMM"). */
class DateTimeFormatter private constructor(private val pattern: String, private val locale: Locale) {

    fun format(date: LocalDate): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        val month = date.month.getDisplayName(TextStyle.FULL, locale)
        val monthShort = date.month.getDisplayName(TextStyle.SHORT, locale)
        return when (pattern) {
            "dd/MM/yyyy" -> "${date.dayOfMonth.toString().padStart(2, '0')}/" +
                "${date.monthValue.toString().padStart(2, '0')}/${date.year.toString().padStart(4, '0')}"
            "EEEE, d 'de' MMMM" -> "$weekday, ${date.dayOfMonth} de $month"
            "d 'de' MMMM 'de' yyyy" -> "${date.dayOfMonth} de $month de ${date.year}"
            "MM/yyyy" -> "${date.monthValue.toString().padStart(2, '0')}/${date.year}"
            "MMM" -> monthShort
            "EEEE" -> weekday
            else -> date.toString()
        }
    }

    fun format(dateTime: LocalDateTime): String = format(dateTime.date)

    companion object {
        fun ofPattern(pattern: String, locale: Locale = Locale.US): DateTimeFormatter =
            DateTimeFormatter(pattern, locale)

        fun ofPattern(pattern: String): DateTimeFormatter = ofPattern(pattern, Locale.US)
    }
}
