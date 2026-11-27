package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.data.r2
import java.time.YearMonth
import kotlin.math.roundToInt

data class RetrospectiveMonth(
    val month: YearMonth,
    val income: Double,
    val expense: Double,
    val transactionCount: Int,
    val categories: List<Pair<String, Double>>
) {
    val result: Double get() = (income - expense).r2()
}

data class RetrospectiveYear(
    val year: Int,
    val months: List<RetrospectiveMonth>
) {
    val income: Double get() = months.sumOf { it.income }.r2()
    val expense: Double get() = months.sumOf { it.expense }.r2()
    val result: Double get() = (income - expense).r2()
    val transactionCount: Int get() = months.sumOf { it.transactionCount }
    val activeMonths: Int get() = months.count { it.transactionCount > 0 }
    val categories: List<Pair<String, Double>>
        get() = months.flatMap { it.categories }
            .groupBy { it.first }
            .map { (category, values) -> category to values.sumOf { it.second }.r2() }
            .sortedByDescending { it.second }
}

private fun TxItem.retrospectiveMonth(): YearMonth? =
    runCatching { YearMonth.parse(tx.date.take(7)) }.getOrNull()

private fun List<TxItem>.retrospectiveEntries(): List<TxItem> =
    filter { !it.ignored && !it.tx.pending && it.tx.amount != 0.0 && it.retrospectiveMonth() != null }

fun monthlyRetrospective(items: List<TxItem>, month: YearMonth): RetrospectiveMonth {
    val entries = items.retrospectiveEntries().filter { it.retrospectiveMonth() == month }
    return summarizeMonth(month, entries)
}

private fun summarizeMonth(month: YearMonth, entries: List<TxItem>): RetrospectiveMonth {
    val categories = entries.filter { it.tx.amount < 0 }
        .groupBy { it.category }
        .map { (category, txs) -> category to (-txs.sumOf { it.tx.amount }).r2() }
        .sortedByDescending { it.second }
    return RetrospectiveMonth(
        month = month,
        income = entries.filter { it.tx.amount > 0 }.sumOf { it.tx.amount }.r2(),
        expense = entries.filter { it.tx.amount < 0 }.sumOf { -it.tx.amount }.r2(),
        transactionCount = entries.size,
        categories = categories
    )
}

fun yearlyRetrospective(items: List<TxItem>, year: Int): RetrospectiveYear {
    val byMonth = items.retrospectiveEntries()
        .mapNotNull { item -> item.retrospectiveMonth()?.takeIf { it.year == year }?.let { it to item } }
        .groupBy({ it.first }, { it.second })
    return RetrospectiveYear(
        year,
        (1..12).map { month ->
            val period = YearMonth.of(year, month)
            summarizeMonth(period, byMonth[period].orEmpty())
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrospectiveSheet(items: List<TxItem>, onDismiss: () -> Unit) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var selectedMonthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedYear by rememberSaveable { mutableIntStateOf(YearMonth.now().year) }
    val selectedMonth = remember(selectedMonthText) {
        runCatching { YearMonth.parse(selectedMonthText) }.getOrDefault(YearMonth.now())
    }
    val monthSummary = remember(items, selectedMonth) { monthlyRetrospective(items, selectedMonth) }
    val previousMonth = remember(items, selectedMonth) { monthlyRetrospective(items, selectedMonth.minusMonths(1)) }
    val yearSummary = remember(items, selectedYear) { yearlyRetrospective(items, selectedYear) }
    val latestMonth = YearMonth.now()
    val currentYear = latestMonth.year

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxHeight(0.9f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Sua retrospectiva", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Um olhar leve sobre o que aconteceu com seu dinheiro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Mês") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Ano") })
            }

            if (mode == 0) {
                PeriodSelector(
                    label = monthLabel(selectedMonth.toString()),
                    canGoForward = selectedMonth < latestMonth,
                    onBack = { selectedMonthText = selectedMonth.minusMonths(1).toString() },
                    onForward = { selectedMonthText = selectedMonth.plusMonths(1).toString() }
                )
                MonthRetrospectiveContent(monthSummary, previousMonth)
            } else {
                PeriodSelector(
                    label = selectedYear.toString(),
                    canGoForward = selectedYear < currentYear,
                    onBack = { selectedYear -= 1 },
                    onForward = { selectedYear += 1 }
                )
                YearRetrospectiveContent(yearSummary)
            }

            Text(
                "Calculado no aparelho com os lançamentos registrados. Meses com histórico incompleto podem mostrar apenas os dados disponíveis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodSelector(label: String, canGoForward: Boolean, onBack: () -> Unit, onForward: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Período anterior") }
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Próximo período")
        }
    }
}

@Composable
private fun MonthRetrospectiveContent(current: RetrospectiveMonth, previous: RetrospectiveMonth) {
    if (current.transactionCount == 0) {
        EmptyRetrospective()
        return
    }
    NCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Text("Despesas no mês", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(current.expense), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryMetric("Receitas", current.income, IncomeColor, Modifier.weight(1f))
            SummaryMetric("Resultado", current.result, if (current.result >= 0) IncomeColor else ExpenseColor, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("${current.transactionCount} lançamentos considerados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    NCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Text("Em relação ao mês anterior", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(monthComparison(current, previous), style = MaterialTheme.typography.bodyMedium)
    }
    CategoryBreakdown(current.categories)
}

@Composable
private fun YearRetrospectiveContent(summary: RetrospectiveYear) {
    if (summary.transactionCount == 0) {
        EmptyRetrospective()
        return
    }
    NCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Text("Despesas no ano", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(summary.expense), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryMetric("Receitas", summary.income, IncomeColor, Modifier.weight(1f))
            SummaryMetric("Resultado", summary.result, if (summary.result >= 0) IncomeColor else ExpenseColor, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${summary.transactionCount} lançamentos · ${summary.activeMonths} ${if (summary.activeMonths == 1) "mês com dados" else "meses com dados"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    NCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Text("Como o ano se distribuiu", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        val maxExpense = summary.months.maxOfOrNull { it.expense }?.coerceAtLeast(1.0) ?: 1.0
        summary.months.forEach { month ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(shortMonth(month.month), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(34.dp))
                NProgress((month.expense / maxExpense).toFloat(), ExpenseColor, Modifier.weight(1f), height = 7.dp)
                Text(money(month.expense), style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(min = 70.dp))
            }
        }
        summary.months.filter { it.expense > 0 }.maxByOrNull { it.expense }?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                "Maior volume de despesas: ${monthLabel(it.month.toString())}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    CategoryBreakdown(summary.categories)
}

@Composable
private fun SummaryMetric(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(money(value), style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CategoryBreakdown(categories: List<Pair<String, Double>>) {
    if (categories.isEmpty()) return
    NCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Text("Onde mais saiu", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val max = categories.first().second.coerceAtLeast(1.0)
        categories.take(3).forEach { (category, amount) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(min = 92.dp, max = 140.dp))
                NProgress((amount / max).toFloat(), categoryColor(category), Modifier.weight(1f), height = 7.dp)
                Text(money(amount), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun EmptyRetrospective() {
    NCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        Text("Ainda sem lançamentos neste período", style = MaterialTheme.typography.titleMedium)
        Text(
            "Quando houver movimentações registradas, seu resumo aparece aqui. Sem notificações ou lembretes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun monthComparison(current: RetrospectiveMonth, previous: RetrospectiveMonth): String {
    if (previous.transactionCount == 0) return "Não há lançamentos no mês anterior para comparar."
    if (previous.expense == 0.0) {
        return if (current.expense > 0.0) "Houve despesas neste mês; no anterior não havia despesas registradas."
        else "Não houve despesas registradas nos dois meses."
    }
    val change = ((current.expense - previous.expense) / previous.expense * 100).roundToInt()
    return when {
        change > 0 -> "As despesas ficaram ${change}% acima do mês anterior."
        change < 0 -> "As despesas ficaram ${-change}% abaixo do mês anterior."
        else -> "As despesas ficaram no mesmo nível do mês anterior."
    }
}
