package java.time

import java.time.format.TextStyle
import java.util.Locale

enum class Month {
    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE,
    JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    val value: Int get() = ordinal + 1

    fun getDisplayName(style: TextStyle, locale: Locale): String {
        val pt = locale.language == "pt"
        val full = arrayOf(
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
        )
        val short = arrayOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
        val enFull = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val enShort = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val source = when {
            style == TextStyle.FULL && pt -> full
            style == TextStyle.FULL -> enFull
            pt -> short
            else -> enShort
        }
        return source[ordinal]
    }

    companion object {
        fun of(value: Int): Month = entries[value - 1]
        fun from(value: Int): Month = of(value)
    }
}
