package com.japaneixxx.freya.ui

import com.japaneixxx.freya.data.CAT_CARD_PAYMENT
import java.time.LocalDate
import java.time.YearMonth

/**
 * Ciclo de fatura de um cartão: de [start] até [end] (inclusive). A fatura fecha em [closing] (= end + 1):
 * compras feitas no próprio dia do fechamento entram no ciclo seguinte.
 */
data class Cycle(val start: LocalDate, val end: LocalDate, val closing: LocalDate)

private fun closingIn(ym: YearMonth, day: Int): LocalDate = ym.atDay(minOf(day.coerceIn(1, 31), ym.lengthOfMonth()))

/** O ciclo que contém [date], dado o dia do mês em que a fatura fecha. */
fun cycleContaining(date: LocalDate, closingDay: Int): Cycle {
    val ym = YearMonth.from(date)
    val thisClose = closingIn(ym, closingDay)
    return if (date.isBefore(thisClose)) {
        Cycle(closingIn(ym.minusMonths(1), closingDay), thisClose.minusDays(1), thisClose)
    } else {
        val next = closingIn(ym.plusMonths(1), closingDay)
        Cycle(thisClose, next.minusDays(1), next)
    }
}

/** O ciclo anterior a [c]. */
fun previousCycle(c: Cycle, closingDay: Int): Cycle = cycleContaining(c.start.minusDays(1), closingDay)

/** Ciclo atual primeiro, depois os [count] - 1 anteriores. */
fun recentCycles(today: LocalDate, closingDay: Int, count: Int = 6): List<Cycle> {
    val out = mutableListOf(cycleContaining(today, closingDay))
    while (out.size < count) out += previousCycle(out.last(), closingDay)
    return out
}

/** Vencimento estimado do ciclo: o primeiro dia com o mesmo dia do mês do vencimento atual, depois do fechamento. */
fun estimatedDue(c: Cycle, dueDay: Int): LocalDate {
    val sameMonth = closingIn(YearMonth.from(c.closing), dueDay)
    return if (sameMonth.isAfter(c.closing)) sameMonth else closingIn(YearMonth.from(c.closing).plusMonths(1), dueDay)
}

data class CycleSummary(
    val cycle: Cycle,
    val items: List<TxItem>,
    /** Quanto a fatura soma: compras menos estornos. Positivo = a pagar. */
    val total: Double,
    val byCategory: List<Pair<String, Double>>
)

/** Soma o ciclo com os lançamentos do cartão. Pagamentos da própria fatura não entram na conta. */
fun summarizeCycle(cardItems: List<TxItem>, cycle: Cycle): CycleSummary {
    val inCycle = cardItems.filter { item ->
        val d = runCatching { LocalDate.parse(item.tx.date) }.getOrNull() ?: return@filter false
        !d.isBefore(cycle.start) && !d.isAfter(cycle.end)
    }.sortedByDescending { it.tx.date }
    val counted = inCycle.filter { it.category != CAT_CARD_PAYMENT }
    val total = -counted.sumOf { it.tx.amount }
    val cats = counted.filter { it.tx.amount < 0 }
        .groupBy { it.category }.map { (k, l) -> k to -l.sumOf { it.tx.amount } }
        .sortedByDescending { it.second }
    return CycleSummary(cycle, inCycle, total, cats)
}
