package java.time.temporal

import java.time.LocalDate
import java.time.YearMonth

enum class ChronoUnit {
    NANOS, MICROS, MILLIS, SECONDS, MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS, YEARS, DECADES, CENTURIES, MILLENNIA, ERAS, FOREVER;

    fun between(start: LocalDate, end: LocalDate): Long = when (this) {
        DAYS -> end.epochDay - start.epochDay
        WEEKS -> (end.epochDay - start.epochDay) / 7
        MONTHS -> monthsBetween(start.year, start.monthValue, end.year, end.monthValue)
        YEARS -> monthsBetween(start.year, start.monthValue, end.year, end.monthValue) / 12
        else -> end.epochDay - start.epochDay
    }

    fun between(start: YearMonth, end: YearMonth): Long = when (this) {
        MONTHS -> end.year.toLong() * 12 + end.monthValue - (start.year.toLong() * 12 + start.monthValue)
        YEARS -> (end.year.toLong() * 12 + end.monthValue - (start.year.toLong() * 12 + start.monthValue)) / 12
        else -> end.year.toLong() * 12 + end.monthValue - (start.year.toLong() * 12 + start.monthValue)
    }

    private fun monthsBetween(y1: Int, m1: Int, y2: Int, m2: Int): Long =
        y2.toLong() * 12 + m2 - (y1.toLong() * 12 + m1)
}
