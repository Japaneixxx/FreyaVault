package app.meufin.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.meufin.data.Bill
import app.meufin.data.Budget
import app.meufin.data.Goal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun PlanScreen(vm: AppViewModel, section: Int, onSection: (Int) -> Unit) {
    val alerts by vm.chargeAlerts.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text("Planejar", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Orçamentos", "Metas", "Contas", "Cartões", "Parcelas", "Recorrentes", "Cobranças").forEachIndexed { i, l ->
                val label = if (i == 6 && alerts.isNotEmpty()) "$l (${alerts.size})" else l
                FilterChip(selected = section == i, onClick = { onSection(i) }, label = { Text(label) })
            }
        }
        when (section) {
            0 -> BudgetsSection(vm)
            1 -> GoalsSection(vm)
            2 -> BillsSection(vm)
            3 -> CardsSection(vm)
            4 -> InstallmentsSection(vm)
            5 -> RecurringSection(vm)
            else -> ChargesSection(vm)
        }
    }
}

// ---------------- Orçamentos ----------------
@Composable
private fun BudgetsSection(vm: AppViewModel) {
    val budgets by vm.budgets.collectAsState()
    val txs by vm.txs.collectAsState()
    var editing by remember { mutableStateOf<Budget?>(null) }
    var creating by remember { mutableStateOf(false) }

    val envelopes by vm.envelopes.collectAsState()
    val real = remember(txs) { txs.filter { !it.ignored } }
    val spent = remember(real) { spendByCategory(real.inMonth(monthKey())).toMap() }
    val eff = remember(budgets, envelopes, real) { effectiveBudgets(budgets, envelopes, real) }
    val totalLimit = eff.sumOf { it.available }
    val totalSpent = eff.sumOf { spent[it.category] ?: 0.0 }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (budgets.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Orçado em ${monthLabel(monthKey())}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${money(totalSpent)} de ${money(totalLimit)}", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                val left = totalLimit - totalSpent
                Text(
                    if (left >= 0) "Ainda disponível: ${money(left)}" else "Acima do orçado: ${money(-left)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val daysLeft = YearMonth.now().lengthOfMonth() - LocalDate.now().dayOfMonth + 1
                if (left > 0 && daysLeft > 0) {
                    Text(
                        "≈ ${money(left / daysLeft)} por dia até o fim do mês",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        if (budgets.isEmpty()) item {
            NCard(Modifier.fillMaxWidth()) {
                Text("Nenhum orçamento ainda.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Gere sugestões com base na média dos últimos 3 meses ou crie os seus.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { creating = true }) { Text("Novo orçamento") }
                OutlinedButton(onClick = { vm.suggestBudgets() }) { Text("Sugerir") }
            }
        }
        items(eff.sortedBy { it.category }, key = { it.category }) { b ->
            NCard(Modifier.fillMaxWidth()) {
                BudgetBar(b.category, spent[b.category] ?: 0.0, b.available) {
                    editing = budgets.firstOrNull { it.category == b.category }
                }
                if (b.envelope) {
                    Text(
                        "✉️ Envelope: ${money(b.limit)} do mês + ${money(b.carry)} acumulados",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (creating) BudgetDialog(null, false, onDismiss = { creating = false }, onSave = { c, v, e -> vm.saveBudget(c, v, e) }, onDelete = null)
    editing?.let { b ->
        BudgetDialog(b, envelopes.any { it.category == b.category }, onDismiss = { editing = null },
            onSave = { c, v, e -> vm.saveBudget(c, v, e) }, onDelete = { vm.deleteBudget(b.category) })
    }
}

@Composable
private fun BudgetDialog(
    existing: Budget?, isEnvelope: Boolean, onDismiss: () -> Unit,
    onSave: (String, Double, Boolean) -> Unit, onDelete: (() -> Unit)?
) {
    var envelope by remember { mutableStateOf(isEnvelope) }
    var cat by remember { mutableStateOf(existing?.category ?: Cats.budget.first()) }
    var value by remember { mutableStateOf(existing?.limitAmount?.roundToInt()?.toString() ?: "") }
    val amount = parseAmount(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo orçamento" else "Editar orçamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing == null) CategoryPicker(cat, Cats.budget) { cat = it }
                else Text(existing.category, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value, { value = it }, label = { Text("Limite mensal (R$)") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Modo envelope")
                        Text(
                            "A sobra de cada mês passa para o seguinte. Estouros não acumulam.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = envelope, onCheckedChange = { envelope = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > 0,
                onClick = { onSave(cat, amount ?: return@TextButton, envelope); onDismiss() }
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

// ---------------- Metas ----------------
@Composable
private fun GoalsSection(vm: AppViewModel) {
    val goals by vm.goals.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Button(onClick = { creating = true }) { Text("Nova meta") } }
        if (goals.isEmpty()) item {
            Text(
                "Crie metas como reserva de emergência, viagem ou entrada de um carro e acompanhe o progresso.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(goals, key = { it.id }) { g ->
            val frac = if (g.target > 0) (g.saved / g.target).toFloat() else 0f
            NCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(g.name, style = MaterialTheme.typography.titleMedium)
                    Text("${(frac * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { frac.coerceIn(0f, 1f) },
                    color = if (frac >= 1f) IncomeColor else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("${money(g.saved)} de ${money(g.target)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { adjusting = g }) { Text("Guardar / retirar") }
                    TextButton(onClick = { vm.deleteGoal(g) }) { Text("Excluir") }
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        val t = parseAmount(target)
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("Nova meta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(target, { target = it }, label = { Text("Valor da meta (R$)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && t != null && t > 0,
                    onClick = { vm.addGoal(name.trim(), t ?: return@TextButton); creating = false }
                ) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancelar") } }
        )
    }

    adjusting?.let { g ->
        var v by remember(g.id) { mutableStateOf("") }
        val a = parseAmount(v)
        AlertDialog(
            onDismissRequest = { adjusting = null },
            title = { Text(g.name) },
            text = { OutlinedTextField(v, { v = it }, label = { Text("Valor (R$)") }, singleLine = true) },
            confirmButton = {
                TextButton(
                    enabled = a != null && a > 0,
                    onClick = { vm.adjustGoal(g, a ?: return@TextButton); adjusting = null }
                ) { Text("Guardar") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        enabled = a != null && a > 0,
                        onClick = { vm.adjustGoal(g, -(a ?: return@TextButton)); adjusting = null }
                    ) { Text("Retirar") }
                    TextButton(onClick = { adjusting = null }) { Text("Cancelar") }
                }
            }
        )
    }
}

// ---------------- Recorrentes ----------------
@Composable
private fun RecurringSection(vm: AppViewModel) {
    val txs by vm.txs.collectAsState()
    val list = remember(txs) { detectRecurring(txs) }
    val total = list.sumOf { it.avg }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Compromisso mensal fixo", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(money(total), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "Cobranças que se repetem todo mês, detectadas automaticamente no seu histórico.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        if (list.isEmpty()) item {
            Text(
                "Ainda não há histórico suficiente. São necessários ao menos 3 meses com a mesma cobrança.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(list, key = { it.name + it.lastDate }) { r ->
            NCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CategoryAvatar(r.category, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text("${r.category} · ${r.months} meses · última em ${dayLabel(r.lastDate)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(money(r.avg), color = ExpenseColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ---------------- Contas a pagar ----------------
@Composable
private fun BillsSection(vm: AppViewModel) {
    val bills by vm.bills.collectAsState()
    var editing by remember { mutableStateOf<Bill?>(null) }
    var creating by remember { mutableStateOf(false) }
    val pending = bills.filter { !isPaid(it) }.sumOf { it.amount }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer) {
                Text("A pagar neste mês", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(money(pending), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "Cadastre aluguel, escola, internet e outras contas fixas. Ative os alertas em Mais para ser avisado antes do vencimento.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        item { Button(onClick = { creating = true }) { Text("Nova conta") } }
        items(bills, key = { it.id }) { b ->
            val paid = isPaid(b)
            val due = billDueDate(b)
            val days = ChronoUnit.DAYS.between(LocalDate.now(), due).toInt()
            val status = when {
                paid -> "Paga em ${monthLabel(monthKey())}"
                days < 0 -> "Atrasada há ${-days} dia(s)"
                days == 0 -> "Vence hoje"
                else -> "Vence em $days dia(s) · dia ${b.dueDay}"
            }
            NCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            status, style = MaterialTheme.typography.bodySmall,
                            color = if (!paid && days < 0) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(money(b.amount), fontWeight = FontWeight.Medium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.setBillPaid(b, !paid) }) { Text(if (paid) "Desfazer" else "Marcar como paga") }
                    TextButton(onClick = { editing = b }) { Text("Editar") }
                }
            }
        }
    }

    if (creating) BillDialog(null, { creating = false }, { vm.saveBill(it) }, null)
    editing?.let { b -> BillDialog(b, { editing = null }, { vm.saveBill(it) }, { vm.deleteBill(b) }) }
}

@Composable
private fun BillDialog(existing: Bill?, onDismiss: () -> Unit, onSave: (Bill) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var value by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var day by remember { mutableStateOf(existing?.dueDay?.toString() ?: "") }
    val amount = parseAmount(value)
    val d = day.trim().toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Nova conta a pagar" else "Editar conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(value, { value = it }, label = { Text("Valor (R$)") }, singleLine = true)
                OutlinedTextField(day, { day = it }, label = { Text("Dia do vencimento (1-31)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && amount != null && amount > 0 && d != null && d in 1..31,
                onClick = {
                    val a = amount ?: return@TextButton
                    val dd = d ?: return@TextButton
                    onSave(Bill(existing?.id ?: 0, name.trim(), a, dd, existing?.lastPaidMonth ?: ""))
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

// ---------------- Cartões ----------------
@Composable
private fun CardsSection(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val cards = accounts.filter { it.type == "CREDIT" }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cards.isEmpty()) item {
            Text("Nenhum cartão de crédito encontrado nas contas conectadas.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(cards, key = { it.id }) { c ->
            val limit = c.creditLimit
            val used = if (limit != null && c.availableLimit != null) (limit - c.availableLimit).coerceAtLeast(0.0) else null
            NCard(Modifier.fillMaxWidth()) {
                Text(c.institution, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Fatura atual")
                    Text(money(c.balance), fontWeight = FontWeight.Medium, color = ExpenseColor)
                }
                c.dueDate?.let {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vencimento", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dayLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (limit != null && used != null && limit > 0) {
                    Spacer(Modifier.height(10.dp))
                    val frac = (used / limit).toFloat()
                    LinearProgressIndicator(
                        progress = { frac.coerceIn(0f, 1f) },
                        color = when { frac >= 0.9f -> ExpenseColor; frac >= 0.7f -> WarnColor; else -> MaterialTheme.colorScheme.primary },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Limite usado ${money(used)} de ${money(limit)} · disponível ${money(c.availableLimit ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------- Parcelas ----------------
@Composable
private fun InstallmentsSection(vm: AppViewModel) {
    val txs by vm.txs.collectAsState()
    val list = remember(txs) { detectInstallments(txs) }
    val perMonth = list.sumOf { it.amount }
    val remaining = list.sumOf { it.remainingAmount }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Compras parceladas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${money(perMonth)} por mês", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Ainda faltam ${money(remaining)} em parcelas futuras.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        if (list.isEmpty()) item {
            Text(
                "Nenhuma compra parcelada detectada. O app procura padrões como 03/10 na descrição da compra.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(list, key = { it.name + it.total + it.lastDate }) { r ->
            NCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text("${r.category} · parcela ${r.current} de ${r.total}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(money(r.amount), color = ExpenseColor, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { r.current.toFloat() / r.total },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text("Restam ${r.remaining} parcela(s): ${money(r.remainingAmount)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------- Cobranças: duplicadas e aumentos ----------------
@Composable
private fun ChargesSection(vm: AppViewModel) {
    val alerts by vm.chargeAlerts.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Cobranças para revisar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    "O app compara seus lançamentos e avisa quando algo foi cobrado duas vezes em poucos dias ou quando uma assinatura/conta fixa ficou mais cara.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        if (alerts.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("✅", style = MaterialTheme.typography.headlineLarge)
                Text("Tudo certo por aqui", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Nenhuma cobrança duplicada ou reajuste encontrado nos últimos meses.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(alerts, key = { it.key }) { a ->
            NCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (a.isDuplicate) "⚠️" else "📈", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            if (a.isDuplicate) "Possível cobrança duplicada" else "Cobrança mais cara que o normal",
                            style = MaterialTheme.typography.labelSmall, color = ExpenseColor
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(a.describe { money(it) }, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.dismissChargeAlert(a.key) }) { Text("Está tudo bem") }
                }
            }
        }
    }
}
