package app.meufin.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meufin.data.ALL_CATEGORIES

@Composable
fun TransactionsScreen(vm: AppViewModel) {
    val txs by vm.txs.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) } // 0 todas, 1 receitas, 2 despesas
    var editing by remember { mutableStateOf<TxItem?>(null) }

    val list = remember(txs, query, filter) {
        txs.filter {
            (query.isBlank() ||
                it.tx.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)) &&
                when (filter) {
                    1 -> it.tx.amount > 0
                    2 -> it.tx.amount < 0
                    else -> true
                }
        }
    }
    val grouped = remember(list) { list.groupBy { it.tx.date } }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Buscar") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todas", "Receitas", "Despesas").forEachIndexed { i, label ->
                FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) })
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            grouped.forEach { (date, rows) ->
                item(key = "h$date") {
                    Text(
                        dayLabel(date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                items(rows, key = { it.tx.id }) { item ->
                    TxRow(item) { editing = item }
                }
            }
        }
    }

    editing?.let { item ->
        AlertDialog(
            onDismissRequest = { editing = null },
            confirmButton = { TextButton(onClick = { editing = null }) { Text("Fechar") } },
            title = { Text("Categoria") },
            text = {
                Column {
                    Text(
                        "Vale para todas as transações com descrição parecida.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(ALL_CATEGORIES) { cat ->
                            Text(
                                cat,
                                fontWeight = if (cat == item.category) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { vm.setCategory(item, cat); editing = null }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        )
    }
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
            Text(
                item.category + if (tx.pending) " · pendente" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            money(tx.amount),
            color = if (item.ignored) MaterialTheme.colorScheme.onSurfaceVariant
            else if (tx.amount >= 0) IncomeColor else ExpenseColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
