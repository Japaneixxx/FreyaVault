package app.meufin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
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
import app.meufin.data.BillPayment
import app.meufin.data.Income
import app.meufin.data.Budget
import app.meufin.data.Goal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Índices das seções da aba Planejar. */
object PlanSection {
    const val BUDGETS = 0
    const val GOALS = 1
    const val COMMITMENTS = 2
    const val AGENDA = 3
    const val CARDS = 4
    const val INSTALLMENTS = 5
    const val RECURRING = 6
    const val CHARGES = 7
}

@Composable
fun PlanScreen(vm: AppViewModel, section: Int, onSection: (Int) -> Unit) {
    val alerts by vm.chargeAlerts.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Text("Planejar", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Orçamentos", "Metas", "Compromissos", "Agenda", "Cartões", "Parcelas", "Recorrentes", "Cobranças").forEachIndexed { i, l ->
                val label = if (i == PlanSection.CHARGES && alerts.isNotEmpty()) "$l (${alerts.size})" else l
                FilterChip(selected = section == i, onClick = { onSection(i) }, label = { Text(label) })
            }
        }
        when (section) {
            PlanSection.BUDGETS -> BudgetsSection(vm)
            PlanSection.GOALS -> GoalsSection(vm)
            PlanSection.COMMITMENTS -> CommitmentsSection(vm)
            PlanSection.AGENDA -> AgendaSection(vm)
            PlanSection.CARDS -> CardsSection(vm)
            PlanSection.INSTALLMENTS -> InstallmentsSection(vm)
            PlanSection.RECURRING -> RecurringSection(vm)
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

// ---------------- Compromissos: fixas, variáveis e financiamentos ----------------
@Composable
private fun KindAvatar(kind: String?, size: androidx.compose.ui.unit.Dp = 42.dp) {
    val (emoji, color) = when (kind) {
        KIND_VARIABLE -> "📊" to WarnColor
        KIND_INSTALLMENT -> "🏦" to ExpenseColor
        else -> "📌" to IncomeColor
    }
    androidx.compose.foundation.layout.Box(
        Modifier.size(size).background(color.copy(alpha = 0.18f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { Text(emoji) }
}

@Composable
private fun CommitmentsSection(vm: AppViewModel) {
    val bills by vm.bills.collectAsState()
    val payments by vm.billPayments.collectAsState()
    var editing by remember { mutableStateOf<Bill?>(null) }
    var creating by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Bill?>(null) }

    val ym = YearMonth.now()
    val active = bills.filter { isBillActive(it, ym) }
    val pendingTotal = active.filter { !isPaid(it) }.sumOf { billEstimate(it, payments) }
    val paidTotal = active.filter { isPaid(it) }.sumOf { b ->
        payments.firstOrNull { it.billId == b.id && it.month == monthKey() }?.amount ?: b.amount
    }
    val frac = if (pendingTotal + paidTotal > 0) (paidTotal / (pendingTotal + paidTotal)).toFloat() else 0f

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                val c = MaterialTheme.colorScheme.onPrimaryContainer
                Text("Compromissos de ${monthLabel(monthKey())}", style = MaterialTheme.typography.labelLarge, color = c)
                Text("Falta pagar ${money(pendingTotal)}", style = MaterialTheme.typography.headlineSmall, color = c)
                Text("Já pago: ${money(paidTotal)}", style = MaterialTheme.typography.bodySmall, color = c)
                Spacer(Modifier.height(10.dp))
                NProgress(frac, IncomeColor)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { vm.checkBillPayments() }) { Text("Verificar pagamentos no extrato") }
            }
        }
        item { Button(onClick = { creating = true }) { Text("+ Novo compromisso") } }
        if (bills.isEmpty()) item {
            Text(
                "Cadastre suas contas do mês: internet (fixa), luz e água (variáveis) e financiamentos com parcelas definidas. Assim o app prevê quanto vai sair e quando.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        listOf(KIND_FIXED to "Contas fixas", KIND_VARIABLE to "Contas variáveis", KIND_INSTALLMENT to "Financiamentos e parcelas")
            .forEach { (kind, title) ->
                val list = bills.filter { (it.kind ?: KIND_FIXED) == kind }.sortedBy { it.dueDay }
                if (list.isNotEmpty()) {
                    item(key = "h_$kind") { SectionTitle(title, Modifier.padding(top = 4.dp)) }
                    items(list, key = { it.id }) { b ->
                        CommitmentCard(
                            b, payments,
                            onToggle = { paid ->
                                if (!paid && b.kind == KIND_VARIABLE) paying = b else vm.setBillPaid(b, !paid)
                            },
                            onEdit = { editing = b }
                        )
                    }
                }
            }
    }

    if (creating) CommitmentSheet(null, { creating = false }) { vm.saveBill(it) }
    editing?.let { b -> CommitmentSheet(b, { editing = null }, onDelete = { vm.deleteBill(b) }) { vm.saveBill(it) } }
    paying?.let { b ->
        var value by remember(b.id) { mutableStateOf("%.2f".format(billEstimate(b, payments))) }
        val amount = parseAmount(value)
        AlertDialog(
            onDismissRequest = { paying = null },
            title = { Text(b.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quanto veio a conta deste mês?", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value, { value = it }, label = { Text("Valor pago (R$)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text(
                        "O app usa a média dos últimos pagamentos para prever os próximos meses.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = amount != null && amount > 0,
                    onClick = { vm.setBillPaid(b, true, amount ?: return@TextButton); paying = null }
                ) { Text("Marcar como paga") }
            },
            dismissButton = { TextButton(onClick = { paying = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun CommitmentCard(b: Bill, payments: List<BillPayment>, onToggle: (paid: Boolean) -> Unit, onEdit: () -> Unit) {
    val ym = YearMonth.now()
    val active = isBillActive(b, ym)
    val paid = isPaid(b)
    val due = billDueDate(b, ym)
    val days = ChronoUnit.DAYS.between(LocalDate.now(), due).toInt()
    val est = billEstimate(b, payments)
    val paidAmount = payments.firstOrNull { it.billId == b.id && it.month == monthKey() }?.amount
    val n = installmentNumber(b, ym)

    val status = when {
        !active && n != null && n < 1 -> "Começa em ${monthLabel(b.firstMonth ?: monthKey())}"
        !active -> "Financiamento concluído"
        paid -> "Paga em ${monthLabel(monthKey())}"
        days < 0 -> "Atrasada há ${-days} dia(s)"
        days == 0 -> "Vence hoje"
        else -> "Vence em $days dia(s) · dia ${b.dueDay}"
    }
    NCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            KindAvatar(b.kind)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    status, style = MaterialTheme.typography.bodySmall,
                    color = if (active && !paid && days < 0) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    (if (b.kind == KIND_VARIABLE && !paid) "~ " else "") + money(if (paid) paidAmount ?: est else est),
                    fontWeight = FontWeight.Medium
                )
                if (b.kind == KIND_VARIABLE && paid && paidAmount != null && paidAmount > est * 1.1) {
                    Text("acima da média", style = MaterialTheme.typography.labelSmall, color = ExpenseColor)
                }
            }
        }
        if (b.kind == KIND_INSTALLMENT && n != null && b.totalInstallments != null) {
            val total = b.totalInstallments
            val left = remainingInstallments(b)
            Spacer(Modifier.height(10.dp))
            NProgress((n.coerceIn(0, total)).toFloat() / total, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Parcela ${n.coerceIn(1, total)} de $total · restam $left (${money(left * b.amount)})" +
                    (installmentEnd(b)?.let { " · termina em ${shortMonth(it)}/${it.year}" } ?: ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!b.matchText.isNullOrBlank()) {
            Text("🔎 Baixa automática: \"${b.matchText}\"", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (active) TextButton(onClick = { onToggle(paid) }) { Text(if (paid) "Desfazer" else "Marcar como paga") }
            TextButton(onClick = onEdit) { Text("Editar") }
        }
    }
}

private val commitmentTemplates = listOf(
    "Internet" to KIND_FIXED, "Aluguel" to KIND_FIXED, "Condomínio" to KIND_FIXED, "Plano de saúde" to KIND_FIXED,
    "Escola" to KIND_FIXED, "Luz" to KIND_VARIABLE, "Água" to KIND_VARIABLE, "Gás" to KIND_VARIABLE,
    "Celular" to KIND_VARIABLE, "Financiamento" to KIND_INSTALLMENT, "Parcela do carro" to KIND_INSTALLMENT
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommitmentSheet(existing: Bill?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (Bill) -> Unit) {
    var kind by remember { mutableStateOf(existing?.kind ?: KIND_FIXED) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var value by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var day by remember { mutableStateOf(existing?.dueDay?.toString() ?: "") }
    var current by remember { mutableStateOf(existing?.let { installmentNumber(it)?.coerceAtLeast(1)?.toString() } ?: "1") }
    var total by remember { mutableStateOf(existing?.totalInstallments?.toString() ?: "") }
    var match by remember { mutableStateOf(existing?.matchText ?: "") }

    val amount = parseAmount(value)
    val d = day.trim().toIntOrNull()
    val cur = current.trim().toIntOrNull()
    val tot = total.trim().toIntOrNull()
    val installmentOk = kind != KIND_INSTALLMENT || (tot != null && tot in 2..480 && cur != null && cur in 1..tot)
    val valid = name.isNotBlank() && amount != null && amount > 0 && d != null && d in 1..31 && installmentOk

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (existing == null) "Novo compromisso" else "Editar compromisso", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == KIND_FIXED, onClick = { kind = KIND_FIXED }, label = { Text("Fixa") })
                FilterChip(selected = kind == KIND_VARIABLE, onClick = { kind = KIND_VARIABLE }, label = { Text("Variável") })
                FilterChip(selected = kind == KIND_INSTALLMENT, onClick = { kind = KIND_INSTALLMENT }, label = { Text("Financiamento") })
            }
            Text(
                when (kind) {
                    KIND_VARIABLE -> "O valor muda todo mês (luz, água, gás). O app usa a média dos últimos pagamentos como previsão."
                    KIND_INSTALLMENT -> "Parcelas definidas (financiamento, empréstimo, carnê). O app conta as parcelas e avisa quando terminar."
                    else -> "O valor é sempre o mesmo (internet, aluguel, escola)."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (existing == null) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    commitmentTemplates.forEach { (n, k) ->
                        AssistChip(onClick = { name = n; kind = k }, label = { Text(n) })
                    }
                }
            }
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value, { value = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(when (kind) { KIND_VARIABLE -> "Valor estimado do mês (R$)"; KIND_INSTALLMENT -> "Valor da parcela (R$)"; else -> "Valor mensal (R$)" })
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                day, { day = it }, label = { Text("Dia do vencimento (1 a 31)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (kind == KIND_INSTALLMENT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        current, { current = it }, label = { Text("Parcela atual") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        total, { total = it }, label = { Text("Total de parcelas") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                if (tot != null && cur != null && cur in 1..tot) {
                    val end = YearMonth.now().plusMonths((tot - cur).toLong())
                    Text(
                        "Faltam ${tot - cur + 1} parcela(s) contando a deste mês, até ${shortMonth(end)}/${end.year}.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedTextField(
                match, { match = it }, label = { Text("Palavra no extrato para baixa automática (opcional)") },
                supportingText = { Text("Ex.: enel, vivo fibra. Quando um débito com esse texto aparecer, a conta é marcada como paga.") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = valid,
                    onClick = {
                        val a = amount ?: return@Button
                        val dd = d ?: return@Button
                        val isInst = kind == KIND_INSTALLMENT
                        onSave(
                            Bill(
                                existing?.id ?: 0, name.trim(), a, dd, existing?.lastPaidMonth ?: "",
                                kind = kind,
                                totalInstallments = if (isInst) tot else null,
                                firstMonth = if (isInst && cur != null) YearMonth.now().minusMonths((cur - 1).toLong()).toString() else null,
                                matchText = match.trim().ifBlank { null }
                            )
                        )
                        onDismiss()
                    }
                ) { Text("Salvar") }
                if (onDelete != null) OutlinedButton(onClick = { onDelete(); onDismiss() }) { Text("Excluir") }
            }
        }
    }
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

// ---------------- Agenda: salário, contas e saldo previsto ----------------
@Composable
private fun AgendaSection(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val bills by vm.bills.collectAsState()
    val payments by vm.billPayments.collectAsState()
    val incomes by vm.incomes.collectAsState()
    var editing by remember { mutableStateOf<Income?>(null) }
    var creating by remember { mutableStateOf(false) }

    val bank = accounts.filter { it.type == "BANK" }.sumOf { it.balance }
    val agenda = remember(bank, bills, payments, incomes, accounts) { buildAgenda(bank, bills, payments, incomes, accounts) }
    val endOfMonth = YearMonth.now().atEndOfMonth()
    val endBalance = agenda.lastOrNull { !it.date.isAfter(endOfMonth) }?.balanceAfter ?: bank
    val incomeLeft = agenda.filter { it.type == "INCOME" && !it.date.isAfter(endOfMonth) }.sumOf { it.amount }
    val outLeft = -agenda.filter { it.amount < 0 && !it.date.isAfter(endOfMonth) }.sumOf { it.amount }
    val firstNegative = agenda.firstOrNull { it.balanceAfter < 0 }
    val lowest = agenda.minByOrNull { it.balanceAfter }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                val c = MaterialTheme.colorScheme.onPrimaryContainer
                Text("Saldo previsto no fim do mês", style = MaterialTheme.typography.labelLarge, color = c)
                Text(money(endBalance), style = MaterialTheme.typography.headlineSmall, color = c)
                Text(
                    "Hoje ${money(bank)} + receitas ${money(incomeLeft)} − compromissos ${money(outLeft)}",
                    style = MaterialTheme.typography.bodySmall, color = c
                )
                if (lowest != null && firstNegative == null) {
                    Text(
                        "Menor saldo previsto: ${money(lowest.balanceAfter)} em ${dayLabel(lowest.date.toString())}",
                        style = MaterialTheme.typography.bodySmall, color = c
                    )
                }
            }
        }
        if (firstNegative != null) item {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.errorContainer) {
                Text("⚠️ Atenção ao saldo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    "O saldo previsto fica negativo em ${dayLabel(firstNegative.date.toString())}, depois de ${firstNegative.name}: ${money(firstNegative.balanceAfter)}.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Receitas previstas")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cadastre seu salário e outras entradas fixas para o app prever o saldo ao longo do mês.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                incomes.forEach { i ->
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("💰", Modifier.padding(end = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(i.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                (if (i.businessDay) "${i.day}º dia útil" else "dia ${i.day}") +
                                    " · próximo: ${dayLabel(nextIncomeDate(i).toString())}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("+ ${money(i.amount)}", color = IncomeColor, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { editing = i }) { Text("Editar") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { creating = true }) { Text("+ Receita prevista") }
            }
        }

        item { SectionTitle("Próximos 45 dias", Modifier.padding(top = 4.dp)) }
        if (agenda.isEmpty()) item {
            Text(
                "Nada previsto. Cadastre compromissos e receitas para montar a agenda.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        agenda.groupBy { it.date }.forEach { (date, events) ->
            item(key = "d_$date") {
                Column {
                    Text(
                        dayLabel(date.toString()), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                    )
                    NCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                        events.forEachIndexed { idx, e ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(when (e.type) { "INCOME" -> "💰"; "CARD" -> "💳"; else -> "🧾" }, Modifier.padding(end = 12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    e.note?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall,
                                            color = if (it.contains("atrasada")) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                    Text(
                                        (if (e.amount > 0) "+ " else "") + money(e.amount),
                                        color = if (e.amount > 0) IncomeColor else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "saldo ${money(e.balanceAfter)}", style = MaterialTheme.typography.labelSmall,
                                        color = if (e.balanceAfter < 0) ExpenseColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (idx < events.size - 1) HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    if (creating) IncomeSheet(null, { creating = false }) { vm.saveIncome(it) }
    editing?.let { i -> IncomeSheet(i, { editing = null }, onDelete = { vm.deleteIncome(i) }) { vm.saveIncome(it) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeSheet(existing: Income?, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null, onSave: (Income) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "Salário") }
    var value by remember { mutableStateOf(existing?.amount?.let { "%.2f".format(it) } ?: "") }
    var businessDay by remember { mutableStateOf(existing?.businessDay ?: false) }
    var day by remember { mutableStateOf(existing?.day?.toString() ?: "5") }
    val amount = parseAmount(value)
    val d = day.trim().toIntOrNull()
    val dayOk = d != null && d in 1..(if (businessDay) 23 else 31)
    val preview = if (dayOk && d != null) nextIncomeDate(Income(0, name, 0.0, d, businessDay)) else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (existing == null) "Nova receita prevista" else "Editar receita", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(name, { name = it }, label = { Text("Nome (ex.: Salário)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value, { value = it }, label = { Text("Valor líquido (R$)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !businessDay, onClick = { businessDay = false }, label = { Text("Dia fixo do mês") })
                FilterChip(selected = businessDay, onClick = { businessDay = true }, label = { Text("N-ésimo dia útil") })
            }
            OutlinedTextField(
                day, { day = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), isError = !dayOk,
                label = { Text(if (businessDay) "Qual dia útil? (ex.: 5 = quinto dia útil)" else "Dia do mês (1 a 31)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                if (businessDay) "Dias úteis contam de segunda a sexta; feriados não são considerados."
                else "Se o dia cair no fim de semana, o app considera a sexta-feira anterior.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            preview?.let {
                Text("Próximo recebimento previsto: ${dayLabel(it.toString())}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = name.isNotBlank() && amount != null && amount > 0 && dayOk,
                    onClick = {
                        val a = amount ?: return@Button
                        val dd = d ?: return@Button
                        onSave(Income(existing?.id ?: 0, name.trim(), a, dd, businessDay))
                        onDismiss()
                    }
                ) { Text("Salvar") }
                if (onDelete != null) OutlinedButton(onClick = { onDelete(); onDismiss() }) { Text("Excluir") }
            }
        }
    }
}
