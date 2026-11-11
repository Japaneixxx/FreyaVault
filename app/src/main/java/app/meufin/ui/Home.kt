package app.meufin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: AppViewModel, goPlan: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val txs by vm.txs.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val bills by vm.bills.collectAsState()
    val snaps by vm.snapshots.collectAsState()
    var month by rememberSaveable { mutableStateOf(monthKey()) }
    val ym = YearMonth.parse(month)

    val bank = accounts.filter { it.type == "BANK" }.sumOf { it.balance }
    val cards = accounts.filter { it.type == "CREDIT" }.sumOf { it.balance }

    val real = remember(txs) { txs.filter { !it.ignored } }
    val cur = remember(real, month) { real.inMonth(month) }
    val prev = remember(real, month) { real.inMonth(ym.minusMonths(1).toString()) }
    val income = cur.incomeSum()
    val expense = cur.expenseSum()
    val byCat = remember(cur) { spendByCategory(cur) }
    val prevByCat = remember(prev) { spendByCategory(prev).toMap() }

    val curCum = remember(cur, month) {
        val upTo = if (month == monthKey()) LocalDate.now().dayOfMonth else ym.lengthOfMonth()
        cumulative(cur, ym.lengthOfMonth()).take(upTo)
    }
    val prevCum = remember(prev, month) { cumulative(prev, ym.minusMonths(1).lengthOfMonth()) }
    val insights = remember(real, month, Privacy.hidden) { buildInsights(real, month) }
    val last6 = remember(real) {
        (5 downTo 0).map { i ->
            val k = YearMonth.now().minusMonths(i.toLong())
            val l = real.inMonth(k.toString())
            MonthSum(shortMonth(k), l.incomeSum(), l.expenseSum())
        }
    }
    val topTx = remember(cur) { cur.filter { it.tx.amount < 0 }.sortedBy { it.tx.amount }.take(5) }
    val budgetRows = remember(budgets, byCat) {
        budgets.map { b -> Triple(b.category, byCat.firstOrNull { it.first == b.category }?.second ?: 0.0, b.limitAmount) }
            .sortedByDescending { it.second / it.third }.take(3)
    }

    val up = remember(bills, accounts) { upcoming(bills, accounts) }
    val endOfMonth = YearMonth.now().atEndOfMonth()
    val dueUntilEnd = up.filter { !it.date.isAfter(endOfMonth) }.sumOf { it.amount }
    val trend = remember(snaps) { snaps.takeLast(60).map { it.bank } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Visão geral", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.toggleHide() }) { Text(if (Privacy.hidden) "Mostrar" else "Ocultar") }
                    IconButton(onClick = { vm.sync() }, enabled = !vm.syncing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                    }
                }
            }
        }
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                val c = MaterialTheme.colorScheme.onPrimaryContainer
                Text("Saldo em contas", style = MaterialTheme.typography.labelLarge, color = c)
                Spacer(Modifier.height(4.dp))
                Text(money(bank), style = MaterialTheme.typography.headlineLarge, color = c)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cartões (fatura) ${money(cards)}  ·  atualizado ${ago(vm.lastSync)}",
                    style = MaterialTheme.typography.bodySmall, color = c
                )
                if (up.isNotEmpty() && dueUntilEnd > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Previsto no fim do mês: ${money(bank - dueUntilEnd)} (após ${money(dueUntilEnd)} em vencimentos)",
                        style = MaterialTheme.typography.bodySmall, color = c
                    )
                }
            }
        }
        if (up.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Próximos vencimentos")
                Spacer(Modifier.height(8.dp))
                up.take(4).forEach { u ->
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), u.date).toInt()
                    val whenTxt = when {
                        u.overdue -> "atrasada há ${-days} dia(s)"
                        days == 0 -> "vence hoje"
                        days == 1 -> "vence amanhã"
                        else -> "vence em $days dias"
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(whenTxt, style = MaterialTheme.typography.bodySmall,
                                color = if (u.overdue) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(u.amount), fontWeight = FontWeight.Medium)
                    }
                }
                TextButton(onClick = goPlan) { Text("Ver contas e cartões") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = ym.minusMonths(1).toString() }) { Icon(Icons.Default.KeyboardArrowLeft, "Mês anterior") }
                Text(monthLabel(month), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { month = ym.plusMonths(1).toString() }, enabled = month < monthKey()) {
                    Icon(Icons.Default.KeyboardArrowRight, "Próximo mês")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Receitas", money(income), IncomeColor, Modifier.weight(1f))
                StatCard("Despesas", money(expense), ExpenseColor, Modifier.weight(1f))
            }
        }
        item {
            val res = income - expense
            StatCard("Resultado do mês", money(res), if (res >= 0) IncomeColor else ExpenseColor, Modifier.fillMaxWidth())
        }
        if (insights.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer) {
                SectionTitle("Insights")
                Spacer(Modifier.height(8.dp))
                insights.forEach {
                    Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Para onde foi o dinheiro")
                Spacer(Modifier.height(12.dp))
                if (byCat.isEmpty()) {
                    Text("Sem despesas neste mês.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DonutChart(byCat.take(8)) }
                    Spacer(Modifier.height(12.dp))
                    val total = byCat.sumOf { it.second }.coerceAtLeast(1.0)
                    byCat.take(6).forEach { (cat, v) ->
                        val before = prevByCat[cat]
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(categoryColor(cat), CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cat, style = MaterialTheme.typography.bodyMedium)
                                if (before != null && before > 0) {
                                    val d = ((v - before) / before * 100).roundToInt()
                                    if (abs(d) >= 5) Text(
                                        (if (d > 0) "▲ " else "▼ ") + "${abs(d)}% vs mês anterior",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (d > 0) ExpenseColor else IncomeColor
                                    )
                                }
                            }
                            Text("${(v / total * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(money(v), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Ritmo de gastos")
                Spacer(Modifier.height(4.dp))
                Text("Linha forte: este mês · linha suave: mês anterior", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                CumulativeChart(curCum, prevCum, maxOf(ym.lengthOfMonth(), prevCum.size))
            }
        }
        if (trend.size >= 2) item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Evolução do saldo")
                Spacer(Modifier.height(4.dp))
                Text("Uma foto por dia em que você sincroniza.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                TrendChart(trend)
            }
        }
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Orçamentos")
                Spacer(Modifier.height(8.dp))
