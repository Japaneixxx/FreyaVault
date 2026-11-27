package java.time

import com.japaneixxx.freya.web.WebBridge
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/** Implementação mínima de LocalDate para o navegador (datas ISO yyyy-MM-dd). */
data class LocalDate(val year: Int, val monthValue: Int, val dayOfMonth: Int) : Comparable<LocalDate> {

    override fun compareTo(other: LocalDate): Int = epochDay.compareTo(other.epochDay)

    val month: Month get() = Month.of(monthValue)
    val dayOfWeek: DayOfWeek get() = DayOfWeek.of((((epochDay + 3) % 7 + 7) % 7 + 1).toInt())

    val epochDay: Long get() = daysFromCivil(year, monthValue, dayOfMonth)

    override fun toString(): String =
        year.toString().padStart(4, '0') + "-" +
            monthValue.toString().padStart(2, '0') + "-" +
            dayOfMonth.toString().padStart(2, '0')

    fun isBefore(other: LocalDate): Boolean = epochDay < other.epochDay
    fun isAfter(other: LocalDate): Boolean = epochDay > other.epochDay
    fun isEqual(other: LocalDate): Boolean = epochDay == other.epochDay

    fun plusDays(days: Long): LocalDate = ofEpochDay(epochDay + days)
    fun minusDays(days: Long): LocalDate = ofEpochDay(epochDay - days)
    fun plusMonths(months: Long): LocalDate = plusMonthsInternal(months)
    fun minusMonths(months: Long): LocalDate = plusMonthsInternal(-months)
    fun plusYears(years: Long): LocalDate = copy(year = year + years.toInt()).let {
        if (it.monthValue == 2 && it.dayOfMonth == 29 && !isLeap(it.year)) it.copy(dayOfMonth = 28) else it
    }
    fun minusYears(years: Long): LocalDate = plusYears(-years)

    private fun plusMonthsInternal(months: Long): LocalDate {
        val total = year.toLong() * 12 + (monthValue - 1) + months
        val y = floor(total / 12.0).toInt()
        val m = (total - y * 12).toInt() + 1
        val day = minOf(dayOfMonth, lengthOfMonth(y, m))
        return LocalDate(y, m, day)
    }

    fun withDayOfMonth(day: Int): LocalDate = copy(dayOfMonth = day.coerceIn(1, lengthOfMonth()))
    fun lengthOfMonth(): Int = lengthOfMonth(year, monthValue)

    fun atStartOfDay(): LocalDateTime = LocalDateTime(this, 0, 0, 0, 0)
    fun atStartOfDay(zone: ZoneId): ZonedDateTime = ZonedDateTime(this, epochDay * 86_400_000L)
    fun format(formatter: DateTimeFormatter): String = formatter.format(this)
    fun atYear(y: Int): LocalDate = copy(year = y)

    fun until(end: LocalDate, unit: ChronoUnit): Long = unit.between(this, end)

    companion object {
        fun now(): LocalDate = parse(WebBridge.todayIso())

        fun of(year: Int, month: Int, dayOfMonth: Int): LocalDate {
            require(month in 1..12) { "Mês inválido: $month" }
            require(dayOfMonth in 1..lengthOfMonth(year, month)) { "Dia inválido: $dayOfMonth" }
            return LocalDate(year, month, dayOfMonth)
        }

        fun ofEpochDay(day: Long): LocalDate {
            val (y, m, d) = civilFromDays(day)
            return LocalDate(y, m, d)
        }

        fun parse(text: String): LocalDate {
            val parts = text.trim().take(10).split('-')
            require(parts.size >= 3) { "Data inválida: $text" }
            return of(parts[0].toInt(), parts[1].toInt(), parts[2].take(2).toInt())
        }

        fun from(text: String): LocalDate = parse(text)

        internal fun lengthOfMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeap(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        private fun isLeap(year: Int): Boolean =
            year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}

private fun isLeap(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun daysFromCivil(y0: Int, m: Int, d: Int): Long {
    val y = y0 - if (m <= 2) 1 else 0
    val era = if (y >= 0) y else y - 399
    val yoe = y - era * 400
    val mp = if (m > 2) m - 3 else m + 9
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era.toLong() * 146097 + doe - 719468
}

private fun civilFromDays(z0: Long): Triple<Int, Int, Int> {
    val z = z0 + 719468
    val era = if (z >= 0) z else z - 146096
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var y = (yoe + era * 400).toInt()
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (mp + if (mp < 10) 3 else -9).toInt()
    if (m <= 2) y += 1
    return Triple(y, m, d)
}
