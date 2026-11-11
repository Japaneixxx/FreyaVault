package app.meufin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meufin.data.ALL_CATEGORIES
import java.time.LocalDate

@Composable
fun ExtratoScreen(vm: AppViewModel) {
    val txs by vm.txs.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var query by remember { mutableStateOf("") }
    var type by remember { mutableIntStateOf(0) }      // 0 todas, 1 receitas, 2 despesas
    var period by remember { mutableIntStateOf(1) }    // 0 tudo, 1 este mês, 2 mês passado
    var category by remember { mutableStateOf<String?>(null) }
    var accountLabel by remember { mutableStateOf("Todas as contas") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }

    val accLabels = remember(accounts) { accounts.associate { it.id to "${it.institution} · ${it.name}" } }
    val accOptions = remember(accLabels) { listOf("Todas as contas") + accLabels.values + "Manual" }

    val list = remember(txs, query, type, period, category, accountLabel) {
        val m = when (period) { 1 -> monthKey(); 2 -> monthKey(1); else -> null }
        txs.filter {
            (accountLabel == "Todas as contas" ||
                (accountLabel == "Manual" && it.manual) ||
                accLabels[it.tx.accountId] == accountLabel) &&
                (m == null || it.tx.date.startsWith(m)) &&
                (category == null || it.category == category) &&
                (query.isBlank() || it.tx.description.contains(query, true) ||
                    it.category.contains(query, true) || it.note.contains(query, true)) &&
                when (type) { 1 -> it.tx.amount > 0; 2 -> it.tx.amount < 0; else -> true }
        }
    }
    val grouped = remember(list) { list.groupBy { it.tx.date } }
    val counted = list.filter { !it.ignored }
    val selected = selectedId?.let { id -> txs.firstOrNull { it.tx.id == id } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text("Extrato", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Buscar") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tudo", "Este mês", "Mês passado").forEachIndexed { i, l ->
                    FilterChip(selected = period == i, onClick = { period = i }, label = { Text(l) })
                }
            }
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Todas", "Receitas", "Despesas").forEachIndexed { i, l ->
                    FilterChip(selected = type == i, onClick = { type = i }, label = { Text(l) })
                }
                CategoryPicker(category ?: "Categoria", listOf("Todas") + ALL_CATEGORIES) {
                    category = if (it == "Todas") null else it
                }
            }
            CategoryPicker(accountLabel, accOptions, Modifier.padding(horizontal = 16.dp)) { accountLabel = it }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entradas ${money(counted.incomeSum())}", style = MaterialTheme.typography.bodySmall, color = IncomeColor)
                Text("Saídas ${money(counted.expenseSum())}", style = MaterialTheme.typography.bodySmall, color = ExpenseColor)
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                if (list.isEmpty()) item {
                    Text("Nada por aqui.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                grouped.forEach { (date, rows) ->
                    item(key = "h$date") {
                        Text(
                            dayLabel(date),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(rows, key = { it.tx.id }) { item -> TxRow(item) { selectedId = item.tx.id } }
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, "Novo lançamento") }
    }

    selected?.let { item ->
        val accName = accounts.firstOrNull { it.id == item.tx.accountId }?.let { "${it.institution} · ${it.name}" }
            ?: "Lançamento manual"
        DetailDialog(item, accName, vm) { selectedId = null }
    }
    if (adding) AddDialog(vm) { adding = false }
}

@Composable
private fun TxRow(item: TxItem, onClick: () -> Unit) {
    val tx = item.tx
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(tx.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            val extra = buildString {
                append(item.category)
                if (tx.pending) append(" · pendente")
                if (item.metaIgnored) append(" · ignorada")
                if (item.note.isNotBlank()) append(" · nota")
            }
            Text(extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            money(tx.amount),
            color = if (item.ignored) MaterialTheme.colorScheme.onSurfaceVariant
            else if (tx.amount >= 0) IncomeColor else ExpenseColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailDialog(item: TxItem, accountLabel: String, vm: AppViewModel, onClose: () -> Unit) {
    var note by remember(item.tx.id) { mutableStateOf(item.note) }
    val finish = { vm.setNote(item, note); onClose() }
    AlertDialog(
        onDismissRequest = finish,
        confirmButton = { TextButton(onClick = finish) { Text("Concluir") } },
        dismissButton = {
            if (item.manual) TextButton(onClick = { vm.deleteManual(item.tx); onClose() }) { Text("Excluir") }
        },
        title = { Text(item.tx.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(
                    money(item.tx.amount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (item.tx.amount >= 0) IncomeColor else ExpenseColor
                )
                Text("${dayLabel(item.tx.date)} · $accountLabel", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("Categoria", style = MaterialTheme.typography.labelMedium)
                CategoryPicker(item.category, ALL_CATEGORIES) { vm.setCategory(item, it) }
                Text(
                    "Vale para transações com descrição parecida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Ignorar nos totais")
                    Switch(checked = item.metaIgnored, onCheckedChange = { vm.setIgnored(item, it) })
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Observação") }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun AddDialog(vm: AppViewModel, onClose: () -> Unit) {
    var desc by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var expense by remember { mutableStateOf(true) }
    var cat by remember { mutableStateOf("Outros") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val amount = parseAmount(value)
    val dateOk = runCatching { LocalDate.parse(date) }.isSuccess

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Novo lançamento") },
        confirmButton = {
            TextButton(
                enabled = desc.isNotBlank() && amount != null && amount > 0 && dateOk,
                onClick = {
                    val v = amount ?: return@TextButton
                    vm.addManual(desc.trim(), if (expense) -v else v, date, cat)
                    onClose()
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = expense, onClick = { expense = true }, label = { Text("Despesa") })
                    FilterChip(selected = !expense, onClick = { expense = false }, label = { Text("Receita") })
                }
                OutlinedTextField(desc, { desc = it }, label = { Text("Descrição") }, singleLine = true)
                OutlinedTextField(value, { value = it }, label = { Text("Valor (R$)") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Data (AAAA-MM-DD)") }, singleLine = true, isError = !dateOk)
                CategoryPicker(cat, ALL_CATEGORIES) { cat = it }
            }
        }
    )
}
