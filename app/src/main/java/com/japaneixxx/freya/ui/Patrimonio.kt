package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.data.Investment
import com.japaneixxx.freya.data.NetItem
import com.japaneixxx.freya.data.computeNetWorth
import com.japaneixxx.freya.data.investmentTypeLabel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NetWorthScreen(vm: AppViewModel) {
    var showCalc by rememberSaveable { mutableStateOf(false) }
    if (showCalc) {
        InvestCalcScreen(vm) { showCalc = false }
        return
    }
    val accounts by vm.accounts.collectAsState()
    val investments by vm.investments.collectAsState()
    val netItems by vm.netItems.collectAsState()
    val snaps by vm.netSnapshots.collectAsState()
    var editing by remember { mutableStateOf<NetItem?>(null) }
    var creatingKind by remember { mutableStateOf<String?>(null) }

    val nw = remember(accounts, investments, netItems) { computeNetWorth(accounts, investments, netItems) }
    val trend = remember(snaps) { snaps.takeLast(90).map { it.total } }
    val change = remember(snaps, nw) {
        if (snaps.size < 2) null else {
            val limit = LocalDate.now().minusDays(30).toString()
            val base = snaps.lastOrNull { it.date <= limit } ?: snaps.first()
            val days = runCatching { ChronoUnit.DAYS.between(LocalDate.parse(base.date), LocalDate.now()).toInt() }.getOrDefault(0)
            if (days <= 0) null else (nw.total - base.total) to days
        }
    }
    val assetsParts = remember(nw) {
        listOf("Contas" to nw.bank, "Investimentos" to nw.investments, "Bens" to nw.assets).filter { it.second > 0 }
    }
    val byType = remember(investments) {
        investments.groupBy { investmentTypeLabel(it.type) }
            .map { (t, l) -> t to l }.sortedByDescending { (_, l) -> l.sumOf { it.balance } }
    }
    val invTotal = investments.sumOf { it.balance }
    val invProfit = investments.mapNotNull { it.amountProfit }.takeIf { it.isNotEmpty() }?.sum()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Patrimônio", style = MaterialTheme.typography.headlineMedium) }

        // ---------- total ----------
        item {
            NCard(Modifier.fillMaxWidth().appear(0), container = MaterialTheme.colorScheme.primaryContainer) {
                val c = MaterialTheme.colorScheme.onPrimaryContainer
                Text("Patrimônio líquido", style = MaterialTheme.typography.labelLarge, color = c.copy(alpha = 0.8f))
                AnimatedMoney(
                    value = nw.total,
                    style = MaterialTheme.typography.displaySmall,
                    color = c,
                    durationMillis = 1200
                )
                change?.let { (diff, days) ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        (if (diff >= 0) "▲ " else "▼ ") + "${money(abs(diff))} em $days dia(s)",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = c
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Contas + investimentos + bens − dívidas − faturas de cartão", style = MaterialTheme.typography.labelSmall, color = c.copy(alpha = 0.8f))
            }
        }

        // ---------- composição ----------
        item {
            NCard(Modifier.fillMaxWidth().appear(1)) {
                SectionTitle("Composição")
                Spacer(Modifier.height(10.dp))
                if (assetsParts.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(assetsParts, centerLabel = "Ativos")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                CompRow("Contas", nw.bank, IncomeColor, plus = true)
                CompRow("Investimentos", nw.investments, IncomeColor, plus = true)
                CompRow("Bens", nw.assets, IncomeColor, plus = true)
                CompRow("Dívidas", nw.debts, ExpenseColor, plus = false)
                CompRow("Cartões", nw.cards, ExpenseColor, plus = false)
            }
        }

        if (trend.size >= 2) item {
            NCard(Modifier.fillMaxWidth().appear(2)) {
                SectionTitle("Evolução")
                Spacer(Modifier.height(4.dp))
                Text("Uma foto por dia em que você sincroniza ou altera bens e dívidas.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                TrendChart(trend)
            }
        }

        // ---------- investimentos ----------
        item {
            NCard(Modifier.fillMaxWidth().appear(3)) {
                SectionTitle("Investimentos")
                Spacer(Modifier.height(6.dp))
                AnimatedMoney(invTotal, style = MaterialTheme.typography.headlineSmall)
                invProfit?.let {
                    Text(
                        (if (it >= 0) "Lucro acumulado " else "Prejuízo acumulado ") + money(abs(it)),
                        style = MaterialTheme.typography.bodySmall, color = if (it >= 0) IncomeColor else ExpenseColor
                    )
                }
                if (investments.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Nenhum investimento encontrado. Eles aparecem aqui quando o banco ou a corretora conectada no Meu Pluggy os informa.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (byType.size > 1) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(byType.map { (t, l) -> t to l.sumOf { it.balance } }, centerLabel = "Investido")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showCalc = true }, modifier = Modifier.fillMaxWidth()) { Text("Simular investimento") }
            }
        }
        byType.forEachIndexed { index, (type, list) ->
            item(key = "type_$type") {
                val sub = list.sumOf { it.balance }
                NCard(Modifier.fillMaxWidth().appear(index), padding = 0.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryAvatar(type, 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(type, style = MaterialTheme.typography.titleMedium)
                            if (invTotal > 0) Text("${(sub / invTotal * 100).roundToInt()}% da carteira",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(sub), fontWeight = FontWeight.Medium)
                    }
                    list.forEachIndexed { i, inv ->
                        InvestmentRow(inv)
                        if (i < list.size - 1) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // ---------- bens e dívidas ----------
        item {
            NCard(Modifier.fillMaxWidth().appear(4)) {
                SectionTitle("Bens e dívidas")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cadastre o que o banco não mostra: imóvel, carro, financiamento, empréstimo com amigos.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { creatingKind = "ASSET" }) { Text("+ Bem") }
                    OutlinedButton(onClick = { creatingKind = "DEBT" }) { Text("+ Dívida") }
                }
                netItems.forEachIndexed { index, it0 ->
                    Row(
                        Modifier.fillMaxWidth().appear(index).padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryAvatar(if (it0.kind == "ASSET") "Bens" else "Dívidas", 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(it0.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (it0.kind == "ASSET") "Bem" else "Dívida", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(it0.amount), color = if (it0.kind == "ASSET") IncomeColor else ExpenseColor, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { editing = it0 }) { Text("Editar") }
                    }
                }
            }
        }
    }

    creatingKind?.let { kind ->
        NetItemDialog(null, kind, { creatingKind = null }, { vm.saveNetItem(it) }, null)
    }
    editing?.let { it0 ->
        NetItemDialog(it0, it0.kind, { editing = null }, { vm.saveNetItem(it) }, { vm.deleteNetItem(it0) })
    }
}

@Composable
private fun CompRow(label: String, value: Double, color: androidx.compose.ui.graphics.Color, plus: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        CategoryAvatar(label, 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (value == 0.0) {
            Text("", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                if (plus) "+ " else "− ",
                color = color,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
            AnimatedMoney(
                value = abs(value),
                color = color,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InvestmentRow(inv: Investment) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(inv.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            val rates = listOfNotNull(
                inv.lastTwelveMonthsRate?.let { "12 meses ${pct(it)}" },
                inv.annualRate?.let { "${pct(it)} a.a." }
            ).joinToString(" · ")
            Text(
                inv.institution + if (rates.isNotBlank()) " · $rates" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(money(inv.balance), fontWeight = FontWeight.Medium)
            inv.amountProfit?.let {
                Text(
                    (if (it >= 0) "+ " else "− ") + money(abs(it)),
                    style = MaterialTheme.typography.labelSmall, color = if (it >= 0) IncomeColor else ExpenseColor
                )
            }
        }
    }
}

@Composable
private fun NetItemDialog(
    existing: NetItem?, kind: String, onDismiss: () -> Unit,
    onSave: (NetItem) -> Unit, onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var value by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var isAsset by remember { mutableStateOf(kind == "ASSET") }
    val amount = parseAmount(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo item" else "Editar item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isAsset, onClick = { isAsset = true }, label = { Text("Bem") })
                    FilterChip(selected = !isAsset, onClick = { isAsset = false }, label = { Text("Dívida") })
                }
                OutlinedTextField(name, { name = it }, label = { Text("Nome (ex.: Apartamento, Financiamento)") }, singleLine = true)
                OutlinedTextField(
                    value, { value = it }, label = { Text("Valor atual (R$)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && amount != null && amount >= 0,
                onClick = {
                    val a = amount ?: return@TextButton
                    onSave(NetItem(existing?.id ?: 0, name.trim(), if (isAsset) "ASSET" else "DEBT", a))
                    onDismiss()
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { onDelete(); onDismiss() }) { Text("Excluir") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
