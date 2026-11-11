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
            if (!stable || last < minDate) return@mapNotNull null
            Recurring(l.first().tx.description, l.first().category, avg, months.size, last)
        }
        .sortedByDescending { it.avg }
}

/** Aplica regras de categoria e anotações às transações. */
fun buildItems(txs: List<Tx>, rules: List<Rule>, metas: List<TxMeta>): List<TxItem> {
    val ruleMap = rules.associate { it.ruleKey to it.category }
    val metaMap = metas.associateBy { it.txId }
    return txs.map {
        val m = metaMap[it.id]
        TxItem(it, categorize(it, ruleMap), m?.note.orEmpty(), m?.ignored ?: false)
    }
}

// ---------------- Parcelamentos ----------------
data class Installment(
    val name: String, val category: String, val amount: Double,
    val current: Int, val total: Int, val lastDate: String
) {
    val remaining: Int get() = (total - current).coerceAtLeast(0)
    val remainingAmount: Double get() = remaining * amount
}

private val instRegex = Regex("""(?<!\d)(\d{1,2})\s*/\s*(\d{1,2})(?!\d)""")

/** Detecta compras parceladas pelo padrão "03/10" na descrição. */
fun detectInstallments(txs: List<TxItem>): List<Installment> {
    val minDate = LocalDate.now().minusDays(75).toString()
    data class Hit(val item: TxItem, val cur: Int, val tot: Int)
    val hits = txs.filter { it.tx.amount < 0 && !it.manual }.mapNotNull { t ->
        val m = instRegex.find(t.tx.description) ?: return@mapNotNull null
        val cur = m.groupValues[1].toInt(); val tot = m.groupValues[2].toInt()
        if (tot < 2 || tot > 48 || cur < 1 || cur > tot) null else Hit(t, cur, tot)
    }
    return hits.groupBy { ruleKeyFor(it.item.tx) + "|" + it.tot }
        .mapNotNull { (_, l) ->
            val parc = l.any { it.item.tx.description.contains("parc", ignoreCase = true) }
            if (!parc && l.map { it.item.tx.date.take(7) }.distinct().size < 2) return@mapNotNull null
            val latest = l.maxByOrNull { it.cur } ?: return@mapNotNull null
            val inst = Installment(
                latest.item.tx.description, latest.item.category, -latest.item.tx.amount,
                latest.cur, latest.tot, l.maxOf { it.item.tx.date }
            )
            if (inst.remaining > 0 && inst.lastDate >= minDate) inst else null
        }
        .sortedByDescending { it.remainingAmount }
}

// ---------------- Vencimentos ----------------
data class Upcoming(val name: String, val date: LocalDate, val amount: Double, val kind: String, val overdue: Boolean)

fun billDueDate(b: Bill, ym: YearMonth = YearMonth.now()): LocalDate =
    ym.atDay(minOf(b.dueDay, ym.lengthOfMonth()))

fun isPaid(b: Bill) = b.lastPaidMonth == monthKey()

/** Próximos vencimentos (contas a pagar não pagas + faturas de cartão) nos próximos [days] dias. */
fun upcoming(bills: List<Bill>, accounts: List<Account>, days: Long = 15): List<Upcoming> {
    val today = LocalDate.now()
    val out = mutableListOf<Upcoming>()
    bills.filter { !isPaid(it) }.forEach {
        val d = billDueDate(it)
        if (!d.isAfter(today.plusDays(days))) out += Upcoming(it.name, d, it.amount, "Conta", d.isBefore(today))
    }
    accounts.filter { it.type == "CREDIT" && it.dueDate != null && it.balance > 0 }.forEach {
        val d = runCatching { LocalDate.parse(it.dueDate) }.getOrNull() ?: return@forEach
        if (!d.isBefore(today) && !d.isAfter(today.plusDays(days))) {
            out += Upcoming("Fatura ${it.institution}", d, it.balance, "Cartão", false)
        }
    }
    return out.sortedBy { it.date }
}

fun monthSummaryText(real: List<TxItem>, month: String): String {
    val cur = real.inMonth(month)
    val inc = cur.incomeSum(); val exp = cur.expenseSum()
    val sb = StringBuilder("Resumo de ${monthLabel(month)}\n")
    sb.append("Receitas: ${moneyPlain(inc)}\nDespesas: ${moneyPlain(exp)}\nResultado: ${moneyPlain(inc - exp)}\n")
    val top = spendByCategory(cur).take(5)
    if (top.isNotEmpty()) {
        sb.append("\nMaiores categorias:\n")
        top.forEach { (c, v) -> sb.append("• $c: ${moneyPlain(v)}\n") }
    }
    return sb.toString()
}
