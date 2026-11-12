package com.japaneixxx.freya.ui

import com.japaneixxx.freya.data.Account
import com.japaneixxx.freya.data.Bill
import com.japaneixxx.freya.data.BillPayment
import com.japaneixxx.freya.data.Income
import com.japaneixxx.freya.data.OneTimeIncome
import com.japaneixxx.freya.data.Rule
import com.japaneixxx.freya.data.Tx
import com.japaneixxx.freya.data.TxMeta
import com.japaneixxx.freya.data.categorize
import com.japaneixxx.freya.data.ruleKeyFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import com.japaneixxx.freya.data.Budget
import com.japaneixxx.freya.data.Envelope
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
    cur.filter { it.tx.amount < 0 }
        .groupBy { runCatching { LocalDate.parse(it.tx.date).dayOfWeek }.getOrNull() }
        .filterKeys { it != null }
        .mapValues { (_, l) -> -l.sumOf { it.tx.amount } }
        .maxByOrNull { it.value }?.let { (dow, _) ->
            if (e > 0 && cur.size >= 8) out += "Seu dia de maior gasto costuma ser ${weekdayName(dow!!)}."
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
            Recurring(l.first().clean.title, l.first().category, avg, months.size, last)
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
                latest.item.clean.title, latest.item.category, -latest.item.tx.amount,
                latest.cur, latest.tot, l.maxOf { it.item.tx.date }
            )
            if (inst.remaining > 0 && inst.lastDate >= minDate) inst else null
        }
        .sortedByDescending { it.remainingAmount }
}

// ---------------- Compromissos: contas fixas, variáveis e financiamentos ----------------
const val KIND_FIXED = "FIXED"
const val KIND_VARIABLE = "VARIABLE"
const val KIND_INSTALLMENT = "INSTALLMENT"

fun Bill.kindName(): String = when (kind) {
    KIND_VARIABLE -> "Variável"
    KIND_INSTALLMENT -> "Financiamento"
    else -> "Fixa"
}

fun billDueDate(b: Bill, ym: YearMonth = YearMonth.now()): LocalDate =
    ym.atDay(minOf(b.dueDay, ym.lengthOfMonth()))

fun isPaid(b: Bill) = b.lastPaidMonth == monthKey()

/** Número da parcela no mês [ym] (1 = primeira); nulo se não for financiamento. */
fun installmentNumber(b: Bill, ym: YearMonth = YearMonth.now()): Int? {
    if (b.kind != KIND_INSTALLMENT) return null
    val first = runCatching { YearMonth.parse(b.firstMonth ?: return null) }.getOrNull() ?: return null
    return ChronoUnit.MONTHS.between(first, ym).toInt() + 1
}

/** Fixas e variáveis valem sempre; financiamento só entre a 1ª e a última parcela. */
fun isBillActive(b: Bill, ym: YearMonth = YearMonth.now()): Boolean {
    val n = installmentNumber(b, ym) ?: return true
    return n in 1..(b.totalInstallments ?: Int.MAX_VALUE)
}

/** Valor esperado: variáveis usam a média dos últimos 3 pagamentos (ou a estimativa informada). */
fun billEstimate(b: Bill, payments: List<BillPayment>): Double {
    if (b.kind != KIND_VARIABLE) return b.amount
    val last = payments.filter { it.billId == b.id }.sortedByDescending { it.month }.take(3).map { it.amount }
    return if (last.isEmpty()) b.amount else last.average()
}

/** Parcelas que ainda faltam, incluindo a deste mês se ela ainda não foi paga. */
fun remainingInstallments(b: Bill): Int {
    val total = b.totalInstallments ?: return 0
    val n = installmentNumber(b) ?: return 0
    return (total - n + if (isPaid(b)) 0 else 1).coerceIn(0, total)
}

fun installmentEnd(b: Bill): YearMonth? {
    val total = b.totalInstallments ?: return null
    val first = runCatching { YearMonth.parse(b.firstMonth ?: return null) }.getOrNull() ?: return null
    return first.plusMonths((total - 1).toLong())
}

// ---------------- Receitas previstas e agenda financeira ----------------
/** Dia de recebimento no mês: N-ésimo dia útil, ou dia fixo (se cair no fim de semana, antecipa para sexta). */
fun incomeDate(i: Income, ym: YearMonth = YearMonth.now()): LocalDate {
    if (i.businessDay) {
        val n = i.day.coerceIn(1, 23)
        var d = ym.atDay(1)
        var count = 0
        while (d.monthValue == ym.monthValue) {
            if (d.dayOfWeek != DayOfWeek.SATURDAY && d.dayOfWeek != DayOfWeek.SUNDAY) {
                count++
                if (count == n) return d
            }
            d = d.plusDays(1)
        }
        return ym.atEndOfMonth()
    }
    var d = ym.atDay(minOf(i.day.coerceAtLeast(1), ym.lengthOfMonth()))
    while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
    return d
}

/** Próxima data de recebimento a partir de hoje (neste mês ou no seguinte). */
fun nextIncomeDate(i: Income): LocalDate {
    val d = incomeDate(i)
    return if (!d.isBefore(LocalDate.now())) d else incomeDate(i, YearMonth.now().plusMonths(1))
}

data class AgendaEvent(
    val date: LocalDate,
    val name: String,
    val amount: Double,        // com sinal: entrada positiva, saída negativa
    val type: String,          // INCOME | BILL | CARD
    val note: String?,
    val balanceAfter: Double   // saldo previsto nas contas depois deste evento
)

/** Linha do tempo dos próximos [days] dias: receitas, contas, parcelas e faturas, com o saldo previsto. */
fun buildAgenda(
    bank: Double, bills: List<Bill>, payments: List<BillPayment>,
    incomes: List<Income>, accounts: List<Account>, days: Long = 45,
    oneTimeIncomes: List<OneTimeIncome> = emptyList()
): List<AgendaEvent> {
    data class Raw(val date: LocalDate, val name: String, val amount: Double, val type: String, val note: String?)
    val today = LocalDate.now()
    val limit = today.plusDays(days)
    val raw = mutableListOf<Raw>()

    (0..2).map { YearMonth.now().plusMonths(it.toLong()) }.forEach monthLoop@{ ym ->
        bills.forEach billLoop@{ b ->
            if (!isBillActive(b, ym)) return@billLoop
            if (ym == YearMonth.now() && isPaid(b)) return@billLoop
            val due = billDueDate(b, ym)
            if (due.isAfter(limit)) return@billLoop
            val overdue = due.isBefore(today)
            val parts = mutableListOf<String>()
            installmentNumber(b, ym)?.let { n -> parts += "parcela $n/${b.totalInstallments ?: "?"}" }
            if (b.kind == KIND_VARIABLE) parts += "estimado"
            if (overdue) parts += "atrasada"
            raw += Raw(if (overdue) today else due, b.name, -billEstimate(b, payments), "BILL", parts.joinToString(" · ").ifBlank { null })
        }
        incomes.forEach incomeLoop@{ i ->
            val d = incomeDate(i, ym)
            if (!d.isBefore(today) && !d.isAfter(limit)) raw += Raw(d, i.name, i.amount, "INCOME", null)
        }
        oneTimeIncomes.forEach oneTimeLoop@{ i ->
            val d = runCatching { LocalDate.parse(i.date) }.getOrNull() ?: return@oneTimeLoop
            if (!d.isBefore(today) && !d.isAfter(limit)) raw += Raw(d, i.name, i.amount, "INCOME", "Recebimento avulso")
        }
    }
    accounts.filter { it.type == "CREDIT" && it.balance > 0 }.forEach cardLoop@{ a ->
        val d = a.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@cardLoop
        if (!d.isBefore(today) && !d.isAfter(limit)) raw += Raw(d, "Fatura ${a.institution}", -a.balance, "CARD", a.name)
    }

    var balance = bank
    return raw.sortedWith(compareBy<Raw> { it.date }.thenByDescending { it.amount > 0 })
        .map { r ->
            balance += r.amount
            AgendaEvent(r.date, r.name, r.amount, r.type, r.note, balance)
        }
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

// ---------------- Cobranças suspeitas: duplicadas e aumentos ----------------
data class ChargeAlert(
    val key: String,
    val isDuplicate: Boolean,
    val title: String,
    val amount: Double,        // valor cobrado (na duplicada: o valor repetido; no aumento: o novo valor)
    val previous: Double?,     // aumento: valor anterior
    val dateA: String,         // duplicada: 1ª cobrança; aumento: data da cobrança nova
    val dateB: String? = null  // duplicada: 2ª cobrança
) {
    fun describe(fmt: (Double) -> String): String =
        if (isDuplicate) "Duas cobranças de ${fmt(amount)} em ${dayLabel(dateA)} e ${dayLabel(dateB ?: dateA)}."
        else {
            val p = previous ?: amount
            "Subiu de ${fmt(p)} para ${fmt(amount)} (+${((amount - p) / p * 100).roundToInt()}%) em ${dayLabel(dateA)}."
        }
}

private fun median(values: List<Double>): Double {
    val s = values.sorted()
    return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
}

/** Procura cobranças repetidas em poucos dias e assinaturas/contas fixas que ficaram mais caras. */
fun detectChargeAlerts(txs: List<TxItem>): List<ChargeAlert> {
    val today = LocalDate.now()
    val dupMin = today.minusDays(60).toString()
    val incMin = today.minusDays(75).toString()
    val expenses = txs.filter { it.tx.amount < 0 && !it.ignored && !it.manual && !it.tx.pending }
    val out = mutableListOf<ChargeAlert>()

    // 1) mesma conta, mesmo nome e mesmo valor em até 3 dias
    expenses.filter { it.tx.date >= dupMin && -it.tx.amount >= 5.0 }
        .groupBy { Triple(it.tx.accountId, ruleKeyFor(it.tx), it.tx.amount) }
        .forEach { (_, l) ->
            val sorted = l.sortedBy { it.tx.date }
            for (i in 1 until sorted.size) {
                val a = sorted[i - 1]; val b = sorted[i]
                val gap = runCatching {
                    ChronoUnit.DAYS.between(LocalDate.parse(a.tx.date), LocalDate.parse(b.tx.date))
                }.getOrDefault(99L)
                if (gap in 0..3) {
                    out += ChargeAlert("dup_${a.tx.id}_${b.tx.id}", true, b.clean.title, -b.tx.amount, null, a.tx.date, b.tx.date)
                }
            }
        }

    // 2) cobrança mensal estável que subiu de preço na última vez
    expenses.groupBy { ruleKeyFor(it.tx) }.forEach { (key, l) ->
        val byMonth = l.groupBy { it.tx.date.take(7) }.toSortedMap()
        if (byMonth.size < 3 || byMonth.values.any { it.size > 2 }) return@forEach
        val series = byMonth.values.map { m -> m.maxByOrNull { it.tx.date }!! }
        val last = series.last()
        if (last.tx.date < incMin) return@forEach
        val before = series.dropLast(1).takeLast(4).map { -it.tx.amount }
        val base = median(before)
        val stable = before.all { abs(it - base) <= base * 0.10 }
        val now = -last.tx.amount
        if (stable && now > base * 1.04 && now - base >= 1.0) {
            out += ChargeAlert("inc_${key}_${last.tx.id}", false, last.clean.title, now, base, last.tx.date)
        }
    }
    return out.sortedByDescending { it.dateB ?: it.dateA }
}

// ---------------- Envelopes (orçamento com sobra acumulada) ----------------
data class EffectiveBudget(val category: String, val limit: Double, val carry: Double, val envelope: Boolean) {
    val available: Double get() = limit + carry
}

/** Sobra acumulada que entra no mês [upToMonth]: o que não foi gasto em cada mês passa para o seguinte. Estouros não acumulam. */
fun envelopeCarry(category: String, limit: Double, startMonth: String, real: List<TxItem>, upToMonth: String = monthKey()): Double {
    val spentByMonth = real.filter { it.tx.amount < 0 && it.category == category }
        .groupBy { it.tx.date.take(7) }
        .mapValues { (_, l) -> -l.sumOf { it.tx.amount } }
    var ym = runCatching { YearMonth.parse(startMonth) }.getOrNull() ?: return 0.0
    val end = YearMonth.parse(upToMonth)
    var carry = 0.0
    var guard = 0
    while (ym.isBefore(end) && guard++ < 36) {
        carry = (limit + carry - (spentByMonth[ym.toString()] ?: 0.0)).coerceAtLeast(0.0)
        ym = ym.plusMonths(1)
    }
    return carry
}

fun effectiveBudgets(budgets: List<Budget>, envelopes: List<Envelope>, real: List<TxItem>): List<EffectiveBudget> {
    val env = envelopes.associateBy { it.category }
    return budgets.map { b ->
        val e = env[b.category]
        EffectiveBudget(b.category, b.limitAmount, if (e != null) envelopeCarry(b.category, b.limitAmount, e.startMonth, real) else 0.0, e != null)
    }
}
