package app.meufin.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth

@Composable
fun DashboardScreen(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val txs by vm.txs.collectAsState()
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }

    val bank = accounts.filter { it.type == "BANK" }.sumOf { it.balance }
    val cards = accounts.filter { it.type == "CREDIT" }.sumOf { it.balance }

    val real = remember(txs) { txs.filter { !it.ignored } }
    val inMonth = remember(real, month) { real.filter { it.tx.date.startsWith(month) } }
    val income = inMonth.filter { it.tx.amount > 0 }.sumOf { it.tx.amount }
    val expense = -inMonth.filter { it.tx.amount < 0 }.sumOf { it.tx.amount }

    val byCat = remember(inMonth) {
        inMonth.filter { it.tx.amount < 0 }
            .groupBy { it.category }
            .map { (c, l) -> c to -l.sumOf { it.tx.amount } }
            .sortedByDescending { it.second }
    }
    val last6 = remember(real) {
        (5 downTo 0).map { i ->
            val ym = YearMonth.now().minusMonths(i.toLong())
            val l = real.filter { it.tx.date.startsWith(ym.toString()) }
            MonthSum(
                shortMonth(ym),
                l.filter { it.tx.amount > 0 }.sumOf { it.tx.amount },
                -l.filter { it.tx.amount < 0 }.sumOf { it.tx.amount }
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Saldo em contas", style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { vm.sync() }, enabled = !vm.syncing) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                        }
                    }
                    Text(money(bank), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Cartões (fatura): ${money(cards)}  ·  Atualizado ${ago(vm.lastSync)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = YearMonth.parse(month).minusMonths(1).toString() }) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Mês anterior")
                }
                Text(monthLabel(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(
                    onClick = { month = YearMonth.parse(month).plusMonths(1).toString() },
                    enabled = month < YearMonth.now().toString()
                ) { Icon(Icons.Default.KeyboardArrowRight, "Próximo mês") }
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
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Últimos 6 meses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    MonthlyBars(last6)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Gastos por categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (byCat.isEmpty()) {
                        Text("Sem despesas neste mês.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val total = byCat.sumOf { it.second }.coerceAtLeast(1.0)
                        byCat.take(8).forEach { (cat, value) ->
                            val color = categoryColor(cat)
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(color, CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Text(cat, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(money(value), style = MaterialTheme.typography.bodyMedium)
                                }
                                LinearProgressIndicator(
                                    progress = { (value / total).toFloat() },
                                    color = color,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
