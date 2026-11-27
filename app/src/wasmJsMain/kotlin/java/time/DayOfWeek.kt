package java.time

import java.time.format.TextStyle
import java.util.Locale

enum class DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    val value: Int get() = ordinal + 1

    fun getDisplayName(style: TextStyle, locale: Locale): String {
        val pt = locale.language == "pt"
        val fullPt = arrayOf("segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo")
        val shortPt = arrayOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")
        val fullEn = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val shortEn = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return when {
            style == TextStyle.FULL && pt -> fullPt[ordinal]
            style == TextStyle.FULL -> fullEn[ordinal]
            pt -> shortPt[ordinal]
            else -> shortEn[ordinal]
        }
    }

    companion object {
        fun of(value: Int): DayOfWeek = values()[value - 1]
        fun from(value: Int): DayOfWeek = of(value)
    }
}
