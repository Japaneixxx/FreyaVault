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
import app.meufin.data.BUDGET_CATEGORIES
import app.meufin.data.Bill
import app.meufin.data.Budget
import app.meufin.data.Goal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun PlanScreen(vm: AppViewModel) {
    var section by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text("Planejar", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Orçamentos", "Metas", "Contas", "Cartões", "Parcelas", "Recorrentes").forEachIndexed { i, l ->
                FilterChip(selected = section == i, onClick = { section = i }, label = { Text(l) })
            }
        }
        when (section) {
            0 -> BudgetsSection(vm)
            1 -> GoalsSection(vm)
            2 -> BillsSection(vm)
            3 -> CardsSection(vm)
            4 -> InstallmentsSection(vm)
            else -> RecurringSection(vm)
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

    val spent = remember(txs) {
        spendByCategory(txs.filter { !it.ignored }.inMonth(monthKey())).toMap()
    }
    val totalLimit = budgets.sumOf { it.limitAmount }
    val totalSpent = budgets.sumOf { spent[it.category] ?: 0.0 }

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
        items(budgets.sortedBy { it.category }, key = { it.category }) { b ->
            NCard(Modifier.fillMaxWidth()) {
                BudgetBar(b.category, spent[b.category] ?: 0.0, b.limitAmount) { editing = b }
            }
        }
    }

    if (creating) BudgetDialog(null, onDismiss = { creating = false }, onSave = { c, v -> vm.saveBudget(c, v) }, onDelete = null)
    editing?.let { b ->
        BudgetDialog(b, onDismiss = { editing = null }, onSave = { c, v -> vm.saveBudget(c, v) },
            onDelete = { vm.deleteBudget(b.category) })
    }
}

@Composable
private fun BudgetDialog(
    existing: Budget?, onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit, onDelete: (() -> Unit)?
) {
    var cat by remember { mutableStateOf(existing?.category ?: BUDGET_CATEGORIES.first()) }
    var value by remember { mutableStateOf(existing?.limitAmount?.roundToInt()?.toString() ?: "") }
    val amount = parseAmount(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Novo orçamento" else "Editar orçamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing == null) CategoryPicker(cat, BUDGET_CATEGORIES) { cat = it }
                else Text(existing.category, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value, { value = it }, label = { Text("Limite mensal (R$)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null && amount > 0,
                onClick = { onSave(cat, amount ?: return@TextButton); onDismiss() }
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text