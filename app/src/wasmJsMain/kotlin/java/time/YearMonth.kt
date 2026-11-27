package java.time

import java.time.temporal.ChronoUnit
import kotlin.math.floor

/** Mês civil (yyyy-MM), usado como chave dos resumos. */
data class YearMonth(val year: Int, val monthValue: Int) : Comparable<YearMonth> {

    val month: Month get() = Month.of(monthValue)
    fun lengthOfMonth(): Int = LocalDate.lengthOfMonth(year, monthValue)

    override fun compareTo(other: YearMonth): Int = index.compareTo(other.index)
    private val index: Long get() = year.toLong() * 12 + monthValue - 1

    override fun toString(): String = year.toString().padStart(4, '0') + "-" + monthValue.toString().padStart(2, '0')

    fun isBefore(other: YearMonth): Boolean = index < other.index
    fun isAfter(other: YearMonth): Boolean = index > other.index

    fun plusMonths(months: Long): YearMonth = ofIndex(index + months)
    fun minusMonths(months: Long): YearMonth = ofIndex(index - months)
    fun plusYears(years: Long): YearMonth = copy(year = year + years.toInt())
    fun minusYears(years: Long): YearMonth = plusYears(-years)

    fun atDay(day: Int): LocalDate = LocalDate.of(year, monthValue, day)
    fun atEndOfMonth(): LocalDate = atDay(lengthOfMonth())
    fun isValidDay(day: Int): Boolean = day in 1..lengthOfMonth()
    fun until(end: YearMonth, unit: ChronoUnit): Long = unit.between(this, end)

    companion object {
        fun now(): YearMonth {
            val today = LocalDate.now()
            return of(today.year, today.monthValue)
        }

        fun of(year: Int, month: Int): YearMonth {
            require(month in 1..12) { "Mês inválido: $month" }
            return YearMonth(year, month)
        }

        fun parse(text: String): YearMonth {
            val parts = text.trim().take(7).split('-')
            require(parts.size >= 2) { "Mês inválido: $text" }
            return of(parts[0].toInt(), parts[1].toInt())
        }

        fun from(date: LocalDate): YearMonth = of(date.year, date.monthValue)

        private fun ofIndex(index: Long): YearMonth {
            val y = floor(index / 12.0).toInt()
            val m = (index - y * 12).toInt() + 1
            return of(y, m)
        }
    }
}
