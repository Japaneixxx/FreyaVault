package app.meufin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun HomeScreen(vm: AppViewModel, goPlan: () -> Unit, goAlerts: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val txs by vm.txs.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val bills by vm.bills.collectAsState()
    val snaps by vm.snapshots.collectAsState()
    val envelopes by vm.envelopes.collectAsState()
    val alerts by vm.chargeAlerts.collectAsState()
    var month by rememberSaveable { mutableStateOf(monthKey()) }
    var adding by remember { mutableStateOf(false) }
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
    val perDay = remember(cur) {
        cur.filter { it.tx.amount < 0 }
            .groupBy { it.tx.date.takeLast(2).toIntOrNull() ?: 0 }
            .mapValues { (_, l) -> -l.sumOf { it.tx.amount } }
    }
    val insights = remember(real, month, Privacy.hidden) { buildInsights(real, month) }
    val last6 = remember(real) {
        (5 downTo 0).map { i ->
            val k = YearMonth.now().minusMonths(i.toLong())
            val l = real.inMonth(k.toString())
            MonthSum(shortMonth(k), l.incomeSum(), l.expenseSum())
        }
    }
    val topTx = remember(cur) { cur.filter { it.tx.amount < 0 }.sortedBy { it.tx.amount }.take(5) }
    val budgetRows = remember(budgets, envelopes, real, byCat) {
        effectiveBudgets(budgets, envelopes, real)
            .map { b -> Triple(b.category, byCat.firstOrNull { it.first == b.category }?.second ?: 0.0, b.available) }
            .sortedByDescending { it.second / it.third }.take(3)
    }

    val up = remember(bills, accounts) { upcoming(bills, accounts) }
    val endOfMonth = YearMonth.now().atEndOfMonth()
    val dueUntilEnd = up.filter { !it.date.isAfter(endOfMonth) }.sumOf { it.amount }
    val trend = remember(snaps) { snaps.takeLast(60).map { it.bank } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- hero ----------
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface))
                )
            ) {
                Mountains(Modifier.align(Alignment.BottomCenter))
                Column(Modifier.padding(22.dp)) {
                    Text("Saldo em contas", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(Modifier.height(2.dp))
                    Text(money(bank), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cartões (fatura) ${money(cards)}  ·  atualizado ${ago(vm.lastSync)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    if (up.isNotEmpty() && dueUntilEnd > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Previsto no fim do mês: ${money(bank - dueUntilEnd)}",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { vm.sync() }, enabled = !vm.syncing,
                            label = { Text(if (vm.syncing) "Sincronizando…" else "Sincronizar") })
                        AssistChip(onClick = { vm.toggleHide() }, label = { Text(if (Privacy.hidden) "Mostrar" else "Ocultar") })
                        AssistChip(onClick = { adding = true }, label = { Text("+ Lançamento") })
                    }
                    Spacer(Modifier.height(36.dp))
                }
            }
        }
        if (up.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Próximos vencimentos")
                Spacer(Modifier.height(6.dp))
                up.take(4).forEach { u ->
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), u.date).toInt()
                    val whenTxt = when {
                        u.overdue -> "atrasada há ${-days} dia(s)"
                        days == 0 -> "vence hoje"
                        days == 1 -> "vence amanhã"
                        else -> "vence em $days dias"
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(if (u.kind == "Cartão") "Pagamento de fatura" else "Moradia", 38.dp)
                        Spacer(Modifier.width(12.dp))
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

        if (alerts.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth().clickable(onClick = goAlerts), container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
                SectionTitle("Atenção")
                Spacer(Modifier.height(6.dp))
                alerts.take(2).forEach { a ->
                    Text(
                        (if (a.isDuplicate) "⚠️ Possível duplicada: " else "📈 Mais cara: ") + a.title,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
                    )
                    Text(a.describe { money(it) }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    if (alerts.size > 2) "Ver mais ${alerts.size - 2} aviso(s)" else "Revisar cobranças",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ---------- mês ----------
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
                StatCard("Receitas", "▲", money(income), IncomeColor, Modifier.weight(1f))
                StatCard("Despesas", "▼", money(expense), ExpenseColor, Modifier.weight(1f))
            }
        }
        item {
            val res = income - expense
            NCard(Modifier.fillMaxWidth(), container = if (res >= 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Resultado do mês", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(res), style = MaterialTheme.typography.headlineSmall, color = if (res >= 0) IncomeColor else ExpenseColor)
                    }
                    if (income > 0) Pill("${((res / income) * 100).roundToInt()}% da receita")
                }
            }
        }

        // ---------- insights (carrossel) ----------
        if (insights.isNotEmpty()) item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(insights) { text ->
                    Card(
                        Modifier.width(250.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("💡", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }

        // ---------- categorias ----------
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
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(cat, 36.dp)
                            Spacer(Modifier.width(12.dp))
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
                            Column(horizontalAlignment = Alignment.End) {
                                Text(money(v), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${(v / total * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Calendário de gastos")
                Spacer(Modifier.height(4.dp))
                Text("Quanto mais escuro o dia, maior o gasto.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                SpendHeatmap(ym, perDay)
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
                if (budgetRows.isEmpty()) {
                    Text("Defina limites por categoria para acompanhar seus gastos.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = goPlan) { Text("Criar orçamentos") }
                } else {
                    budgetRows.forEach { (cat, spent, limit) -> BudgetBar(cat, spent, limit) }
                    TextButton(onClick = goPlan) { Text("Ver todos") }
                }
            }
        }
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Últimos 6 meses")
                Spacer(Modifier.height(12.dp))
                MonthlyBars(last6)
            }
        }
        if (topTx.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Maiores gastos do mês")
                Spacer(Modifier.height(6.dp))
                topTx.forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(it.category, 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it.clean.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            Text(it.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(money(it.tx.amount), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (adding) AddSheet(vm) { adding = false }
}

@Composable
fun BudgetBar(category: String, spent: Double, limit: Double, onClick: (() -> Unit)? = null) {
    val frac = if (limit > 0) (spent / limit).toFloat() else 0f
    val color = when {
        frac >= 1f -> ExpenseColor
        frac >= 0.8f -> WarnColor
        else -> IncomeColor
    }
    val mod = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Column(mod.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(category, 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(category, style = MaterialTheme.typography.bodyMedium)
                Text("${money(spent)} de ${money(limit)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${(frac * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(8.dp))
        NProgress(frac, color)
        if (frac >= 1f) {
            Text("Estourou em ${money(spent - limit)}", style = MaterialTheme.typography.labelSmall, color = ExpenseColor,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun StatCard(title: String, glyph: String, value: String, color: Color, modifier: Modifier) {
    NCard(modifier) {
        Box(Modifier.size(30.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Text(glyph, color = color, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
