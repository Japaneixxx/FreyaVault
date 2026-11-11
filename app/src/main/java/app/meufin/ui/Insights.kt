package app.meufin.ui

import app.meufin.data.Account
import app.meufin.data.Bill
import app.meufin.data.Rule
import app.meufin.data.Tx
import app.meufin.data.TxMeta
import app.meufin.data.categorize
import app.meufin.data.ruleKeyFor
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

fun monthKey(offset: Int = 0): String = YearMonth.now().minusMonths(offset.toLong()).toString()

fun List<TxItem>.inMonth(ym: String) = filter { it.tx.date.startsWith(ym) }
fun List<TxItem>.incomeSum() = filter { it.tx.amount > 0 }.sumOf { it.tx.amount }
fun List<TxItem>.expenseSum() = -filter { it.tx.amount < 0 }.sumOf { it.tx.amount }

fun spendByCategory(list: List<TxItem>): List<Pair<String, Double>> =
    list.filter { it.tx.amount < 0 }
        .groupBy { it.category }
        .map { (c, l) -> c to -l.sumOf { it.tx.amount } }
        .sortedByDescending { it.second }

/** Gasto acumulado por dia do mês (índice 0 = dia 1). */
fun cumulative(list: List<TxItem>, days: Int): List<Float> {
    val perDay = FloatArray(days)
    list.filter { it.tx.amount < 0 }.forEach {
        val d = it.tx.date.takeLast(2).toIntOrNull() ?: return@forEach
        if (d in 1..days) perDay[d - 1] += (-it.tx.amount).toFloat()
    }
    var acc = 0f
    return perDay.map { acc += it; acc }
}

fun buildInsights(real: List<TxItem>, month: String): List<String> {
    val ym = YearMonth.parse(month)
    val cur = real.inMonth(month)
    val prev = real.inMonth(ym.minusMonths(1).toString())
    val e = cur.expenseSum(); val pe = prev.expenseSum(); val inc = cur.incomeSum()
    val out = mutableListOf<String>()

    if (pe > 0 && e > 0) {
        val d = (e - pe) / pe * 100
        out += if (d >= 0) "Você gastou ${d.roundToInt()}% a mais que no mês anterior."
        else "Você gastou ${(-d).roundToInt()}% a menos que no mês anterior."
    }
    if (inc > 0) {
        val r = (inc - e) / inc * 100
        out += if (r >= 0) "Você guardou ${r.roundToInt()}% da sua receita neste mês."
        else "As despesas superaram a receita em ${money(e - inc)}."
    }
    spendByCategory(cur).firstOrNull()?.let { (cat, v) ->
        out += "Maior categoria: $cat, com ${money(v)} (${(v / e * 100).roundToInt()}% das despesas)."
    }
    if (month == monthKey()) {
        val passed = LocalDate.now().dayOfMonth
        if (passed >= 5 && e > 0) {
            out += "No ritmo atual, o mês fecha com cerca de ${money(e / passed * ym.lengthOfMonth())} em despesas."
        }
    }
    return out
}

data class Recurring(val name: String, val category: String, val avg: Double, val months: Int, val lastDate: String)

/** Detecta cobranças recorrentes: mesma descrição, valor estável, em 3+ meses distintos. */
fun detectRecurring(txs: List<TxItem>): List<Recurring> {
    val minDate = LocalDate.now().minusMonths(2).withDayOfMonth(1).toString()
    return txs.filter { it.tx.amount < 0 && !it.ignored && !it.manual }
        .groupBy { ruleKeyFor(it.tx) }
        .mapNotNull { (_, l) ->
            val months = l.map { it.tx.date.take(7) }.distinct()
            if (months.size < 3 || l.size > months.size * 3 / 2) return@mapNotNull null
            val amounts = l.map { -it.tx.amount }
            val avg = amounts.average()
            val stable = amounts.count { abs(it - avg) <= avg * 0.2 } >= amounts.size * 0.7
            val last = l.maxOf { it.tx.date }
            if (!stable || last 