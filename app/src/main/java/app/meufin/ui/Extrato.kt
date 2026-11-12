package app.meufin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meufin.data.ruleKeyFor
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ExtratoScreen(vm: AppViewModel) {
    val txs by vm.txs.collectAsState()
    val accounts by vm.accounts.collectAsState()

    var query by remember { mutableStateOf("") }
    var type by remember { mutableIntStateOf(0) }                 // 0 todas, 1 receitas, 2 despesas
    var month by remember { mutableStateOf<String?>(monthKey()) } // null = tudo
    var category by remember { mutableStateOf<String?>(null) }
    var accountLabel by remember { mutableStateOf("Todas as contas") }
    var sort by remember { mutableIntStateOf(0) }                 // 0 recentes, 1 maiores gastos, 2 maiores entradas
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var adding by remember { mutableStateOf(false) }

    val accLabels = remember(accounts) { accounts.associate { it.id to "${it.institution} · ${it.name}" } }
    val accOptions = remember(accLabels) { listOf("Todas as contas") + accLabels.values + "Manual" }
    val months = remember { (0..11).map { YearMonth.now().minusMonths(it.toLong()) } }

    val list = remember(txs, query, type, month, category, accountLabel, sort) {
        val base = txs.filter {
            (accountLabel == "Todas as contas" ||
                (accountLabel == "Manual" && it.manual) ||
                accLabels[it.tx.accountId] == accountLabel) &&
                (month == null || it.tx.date.startsWith(month!!)) &&
                (category == null || it.category == category) &&
                (query.isBlank() || it.tx.description.contains(query, true) || it.clean.title.contains(query, true) ||
                    it.category.contains(query, true) || it.note.contains(query, true)) &&
                when (type) { 1 -> it.tx.amount > 0; 2 -> it.tx.amount < 0; else -> true }
        }
        when (sort) {
            1 -> base.sortedBy { it.tx.amount }
            2 -> base.sortedByDescending { it.tx.amount }
            else -> base
        }
    }
    val grouped = remember(list, sort) { if (sort == 0) list.groupBy { it.tx.date } else emptyMap() }
    val counted = list.filter { !it.ignored }
    val income = counted.incomeSum()
    val expense = counted.expenseSum()
    val selected = selectedId?.let { id -> txs.firstOrNull { it.tx.id == id } }
    val selectedItems = list.filter { it.tx.id in selection }
    val selecting = selection.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ---------- topo ----------
            if (selecting) {
                SelectionBar(
                    count = selection.size,
                    onCancel = { selection = emptySet() },
                    onCategory = { vm.setCategoryBulk(selectedItems, it); selection = emptySet() },
                    onIgnore = { vm.setIgnoredBulk(selectedItems, true); selection = emptySet() },
                    onRestore = { vm.setIgnoredBulk(selectedItems, false); selection = emptySet() }
                )
            } else {
                Text(
                    "Extrato", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp)
                )
            }

            TextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Buscar lançamentos, categorias, notas") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Limpar") }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = month == null, onClick = { month = null }, label = { Text("Tudo") }) }
                items(months) { ym ->
                    val key = ym.toString()
                    val label = shortMonth(ym) + if (ym.year != YearMonth.now().year) " ${ym.year % 100}" else ""
                    FilterChip(selected = month == key, onClick = { month = key }, label = { Text(label) })
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChip(selected = type == 1, onClick = { type = if (type == 1) 0 else 1 }, label = { Text("Receitas") }) }
                item { FilterChip(selected = type == 2, onClick = { type = if (type == 2) 0 else 2 }, label = { Text("Despesas") }) }
                item {
<<<<<<< HEAD
                    CategoryFilterChip(category ?: "Categoria", category != null, listOf("Todas") + Cats.all) {
=======
                    ChipPicker(category ?: "Categoria", category != null, listOf("Todas") + Cats.all) {
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
                        category = if (it == "Todas") null else it
                    }
                }
                item {
                    ChipPicker(
                        if (accountLabel == "Todas as contas") "Conta" else accountLabel.substringAfter("· ").ifBlank { accountLabel },
                        accountLabel != "Todas as contas", accOptions
                    ) { accountLabel = it }
                }
                item {
                    ChipPicker(
                        when (sort) { 1 -> "Maiores gastos"; 2 -> "Maiores entradas"; else -> "Recentes" },
                        sort != 0, listOf("Recentes", "Maiores gastos", "Maiores entradas")
                    ) { sort = when (it) { "Maiores gastos" -> 1; "Maiores entradas" -> 2; else -> 0 } }
                }
            }

            // ---------- resumo do período ----------
            NCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), padding = 16.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryItem("Entradas", money(income), IncomeColor)
                    SummaryItem("Saídas", money(expense), ExpenseColor)
                    SummaryItem("Saldo", money(income - expense), MaterialTheme.colorScheme.onSurface)
                }
                if (income + expense > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().height(6.dp).background(Color.Transparent, RoundedCornerShape(50))) {
                        Box(Modifier.weight(income.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(IncomeColor, RoundedCornerShape(50)))
                        Spacer(Modifier.width(3.dp))
                        Box(Modifier.weight(expense.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().background(ExpenseColor, RoundedCornerShape(50)))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${list.size} lançamento(s)" + if (selecting) "" else " · segure para selecionar vários",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---------- lista ----------
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (list.isEmpty()) item { EmptyState() }

                if (sort == 0) {
                    grouped.forEach { (date, rows) ->
                        item(key = date) {
                            DayBlock(date, rows, selection, selecting,
                                onOpen = { selectedId = it.tx.id },
                                onToggle = { selection = if (it.tx.id in selection) selection - it.tx.id else selection + it.tx.id })
                        }
                    }
                } else {
                    item(key = "flat") {
                        NCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                            list.take(300).forEachIndexed { i, item ->
                                TxRow(item, item.tx.id in selection, selecting, showDate = true,
                                    onClick = { if (selecting) selection = toggle(selection, item.tx.id) else selectedId = item.tx.id },
                                    onLong = { selection = toggle(selection, item.tx.id) })
                                if (i < minOf(list.size, 300) - 1) HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { adding = true },
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Default.Add, "Novo lançamento") }
    }

    selected?.let { item ->
        val accName = accLabels[item.tx.accountId] ?: "Lançamento manual"
        DetailSheet(item, txs, accName, vm,
            onSimilar = { query = it; selectedId = null },
            onClose = { selectedId = null })
    }
    if (adding) AddSheet(vm) { adding = false }
}

private fun toggle(set: Set<String>, id: String) = if (id in set) set - id else set + id

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏔️", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text("Nada por aqui", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ajuste os filtros ou toque em + para registrar um lançamento.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectionBar(
    count: Int, onCancel: () -> Unit, onCategory: (String) -> Unit,
    onIgnore: () -> Unit, onRestore: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Cancelar seleção") }
        Text("$count", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        CategoryPicker("Categoria", Cats.all, onSelect = onCategory)
        TextButton(onClick = onIgnore) { Text("Ignorar") }
        TextButton(onClick = onRestore) { Text("Contar") }
    }
}

@Composable
private fun DayBlock(
    date: String, rows: List<TxItem>, selection: Set<String>, selecting: Boolean,
    onOpen: (TxItem) -> Unit, onToggle: (TxItem) -> Unit
) {
    val net = rows.filter { !it.ignored }.sumOf { it.tx.amount }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dayLabel(date), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                money(net), style = MaterialTheme.typography.labelLarge,
                color = if (net >= 0) IncomeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        NCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            rows.forEachIndexed { i, item ->
                TxRow(item, item.tx.id in selection, selecting, showDate = false,
                    onClick = { if (selecting) onToggle(item) else onOpen(item) },
                    onLong = { onToggle(item) })
                if (i < rows.size - 1) HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxRow(
    item: TxItem, selected: Boolean, selecting: Boolean, showDate: Boolean,
    onClick: () -> Unit, onLong: () -> Unit
) {
    val tx = item.tx
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
    Row(
        Modifier.fillMaxWidth().background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selecting) {
            Checkbox(checked = selected, onCheckedChange = null)
            Spacer(Modifier.width(8.dp))
        }
        CategoryAvatar(item.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
<<<<<<< HEAD
            Text(item.clean.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
=======
            Text(tx.description, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    (if (showDate) dayLabel(tx.date).substringAfter(", ") + " · " else "") + item.category,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
<<<<<<< HEAD
                KindPills(item.clean.kind, item.clean.parcel)
=======
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
                if (tx.pending) Pill("pendente")
                if (item.manual) Pill("manual")
                if (item.metaIgnored) Pill("ignorada")
                if (item.note.isNotBlank()) Text("📝", fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            (if (tx.amount > 0) "+ " else "") + money(tx.amount),
            color = when {
                item.ignored -> MaterialTheme.colorScheme.onSurfaceVariant
                tx.amount >= 0 -> IncomeColor
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------------- detalhe ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailSheet(
    item: TxItem, all: List<TxItem>, accountLabel: String, vm: AppViewModel,
    onSimilar: (String) -> Unit, onClose: () -> Unit
) {
    var note by remember(item.tx.id) { mutableStateOf(item.note) }
    val close = { vm.setNote(item, note); onClose() }
    val key = remember(item.tx.id) { ruleKeyFor(item.tx) }
    val similar = remember(all, key) { all.filter { ruleKeyFor(it.tx) == key && !it.ignored } }

    ModalBottomSheet(
        onDismissRequest = close,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryAvatar(item.category, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
<<<<<<< HEAD
                    Text(item.clean.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${dayLabel(item.tx.date)} · $accountLabel", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        KindPills(item.clean.kind, item.clean.parcel)
                    }
=======
                    Text(item.tx.description, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${dayLabel(item.tx.date)} · $accountLabel", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
                }
            }
            Text(
                money(item.tx.amount), style = MaterialTheme.typography.displaySmall,
                color = if (item.tx.amount >= 0) IncomeColor else MaterialTheme.colorScheme.onSurface
            )
            if (item.tx.pending) Pill("Pendente: ainda pode mudar")
<<<<<<< HEAD
            if (item.tx.description.trim() != item.clean.title) {
                Column {
                    Text("Descrição original do banco", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.tx.description, style = MaterialTheme.typography.bodySmall)
                }
            }
=======
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8

            if (similar.size > 1) {
                NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer, padding = 14.dp) {
                    val total = similar.sumOf { it.tx.amount }
                    Text(
                        "${similar.size} lançamentos parecidos · total ${money(total)} · média ${money(total / similar.size)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
<<<<<<< HEAD
                    TextButton(onClick = { onSimilar(item.clean.title.take(18)); onClose() }) { Text("Ver todos no extrato") }
=======
                    TextButton(onClick = { onSimilar(item.tx.description.take(18)); onClose() }) { Text("Ver todos no extrato") }
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
                }
            }

            Column {
                Text("Categoria", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                CategoryPicker(item.category, Cats.all) { vm.setCategory(item, it) }
                Text("Vale para lançamentos com descrição parecida.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ignorar nos totais")
                    Text("Útil para estornos e transferências.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = item.metaIgnored, onCheckedChange = { vm.setIgnored(item, it) })
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Observação") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = close) { Text("Concluir") }
                if (item.manual) OutlinedButton(onClick = { vm.deleteManual(item.tx); onClose() }) { Text("Excluir") }
            }
        }
    }
}

// ---------------- novo lançamento ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(vm: AppViewModel, onClose: () -> Unit) {
    var desc by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var expense by remember { mutableStateOf(true) }
    var cat by remember { mutableStateOf("Outros") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val amount = parseAmount(value)
    val dateOk = runCatching { LocalDate.parse(date) }.isSuccess
    val valid = desc.isNotBlank() && amount != null && amount > 0 && dateOk

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Novo lançamento", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = expense, onClick = { expense = true }, label = { Text("Despesa") })
                FilterChip(selected = !expense, onClick = { expense = false }, label = { Text("Receita") })
            }
            OutlinedTextField(
                value, { value = it }, label = { Text("Valor (R$)") }, singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(desc, { desc = it }, label = { Text("Descrição") }, singleLine = true,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(date, { date = it }, label = { Text("Data (AAAA-MM-DD)") }, singleLine = true,
                isError = !dateOk, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            CategoryPicker(cat, Cats.all) { cat = it }
            Button(
                enabled = valid,
                onClick = {
                    val v = amount ?: return@Button
                    vm.addManual(desc.trim(), if (expense) -v else v, date, cat)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salvar") }
        }
    }
}
