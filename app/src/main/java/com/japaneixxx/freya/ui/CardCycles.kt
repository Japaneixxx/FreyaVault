package com.japaneixxx.freya.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.japaneixxx.freya.data.Account
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun dm(d: LocalDate) = formatNumber(java.util.Locale.US, "%02d/%02d", d.dayOfMonth, d.monthValue)

private fun dueDayOf(card: Account): Int? =
    card.dueDate?.let { runCatching { LocalDate.parse(it).dayOfMonth }.getOrNull() }

private fun statusText(c: Cycle, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, c.closing).toInt()
    return when {
        days > 1 -> "Aberta · fecha em $days dias"
        days == 1 -> "Aberta · fecha amanhã"
        days == 0 -> "Fecha hoje"
        else -> "Fechada em ${dm(c.closing)}"
    }
}

/** Fatura por ciclo dentro do cartão (aba Planejar > Cartões). */
@Composable
fun CardCycleBlock(vm: AppViewModel, card: Account, allTxs: List<TxItem>) {
    val today = LocalDate.now()
    var closingDay by remember(card.id) { mutableStateOf(vm.closingDayFor(card.id)) }
    var editing by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(card.id) { mutableStateOf(false) }
    var selected by rememberSaveable(card.id) { mutableIntStateOf(0) }
    var showAll by remember(card.id, selected) { mutableStateOf(false) }
    val cardItems = remember(allTxs, card.id) { allTxs.filter { it.tx.accountId == card.id } }

    if (editing) ClosingDayDialog(
        current = closingDay,
        bankDay = vm.bankClosingDayFor(card.id),
        onSave = { vm.setCardClosingDay(card.id, it); closingDay = vm.closingDayFor(card.id); selected = 0; editing = false },
        onDismiss = { editing = false }
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
    val day = closingDay
    if (day == null) {
        Text("Fatura por ciclo", style = MaterialTheme.typography.titleSmall)
        Text(
            "Informe o dia em que a fatura deste cartão fecha para ver as compras de cada ciclo.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { editing = true }) { Text("Informar dia de fechamento") }
        return
    }

    val cycles = remember(day, today) { recentCycles(today, day, 6) }
    val idx = selected.coerceIn(0, cycles.lastIndex)
    val summary = remember(cardItems, cycles, idx) { summarizeCycle(cardItems, cycles[idx]) }
    val current = remember(cardItems, cycles) { if (idx == 0) summary else summarizeCycle(cardItems, cycles[0]) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Fatura por ciclo", style = MaterialTheme.typography.titleSmall)
            Text(
                "${statusText(cycles[0], today)} · ${money(current.total)} até agora",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar" else "Ver") }
    }
    if (!expanded) return

    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cycles.forEachIndexed { i, c ->
            FilterChip(
                selected = i == idx, onClick = { selected = i },
                label = { Text(if (i == 0) "Atual · fecha ${dm(c.closing)}" else "Fechou ${dm(c.closing)}") }
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    val c = summary.cycle
    Text("${dm(c.start)} a ${dm(c.end)} · ${statusText(c, today)}", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        money(summary.total), style = MaterialTheme.typography.headlineSmall,
        color = if (summary.total > 0) ExpenseColor else IncomeColor
    )
    dueDayOf(card)?.let { dd ->
        Text(
            "Vencimento " + (if (idx == 0 && card.dueDate != null && !LocalDate.parse(card.dueDate).isBefore(c.closing)) dayLabel(card.dueDate)
            else "por volta de ${dm(estimatedDue(c, dd))}"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (summary.byCategory.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        val total = summary.byCategory.sumOf { it.second }.coerceAtLeast(1.0)
        summary.byCategory.take(5).forEach { (cat, v) ->
            Column(Modifier.padding(vertical = 3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat, style = MaterialTheme.typography.bodySmall)
                    Text(money(v), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(3.dp))
                NProgress((v / total).toFloat(), MaterialTheme.colorScheme.primary)
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    if (summary.items.isEmpty()) {
        Text("Nenhum lançamento neste ciclo.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        val shown = if (showAll) summary.items else summary.items.take(10)
        shown.forEach { it2 ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    it2.tx.date.takeLast(5).split("-").reversed().joinToString("/"),
                    Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    it2.clean.title.ifBlank { it2.tx.description }, Modifier.weight(1f).padding(end = 8.dp),
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    money(it2.tx.amount), style = MaterialTheme.typography.bodySmall,
                    color = if (it2.tx.amount < 0) ExpenseColor else IncomeColor
                )
            }
        }
        if (summary.items.size > 10) TextButton(onClick = { showAll = !showAll }) {
            Text(if (showAll) "Mostrar menos" else "Mostrar todos (${summary.items.size})")
        }
    }
    Text(
        "Compras no dia do fechamento entram na fatura seguinte. O total vem dos lançamentos do extrato e pode diferir do valor oficial do banco.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TextButton(onClick = { editing = true }) { Text("Dia de fechamento: $day") }
}

@Composable
private fun ClosingDayDialog(current: Int?, bankDay: Int?, onSave: (Int?) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current?.toString() ?: "") }
    val day = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dia de fechamento") },
        text = {
            Column {
                Text("Dia do mês em que a fatura fecha (1 a 31). Está no app do banco, junto da data de vencimento.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it.filter(Char::isDigit).take(2) },
                    label = { Text("Dia") }, singleLine = true,
                    isError = text.isNotEmpty() && (day == null || day !in 1..31),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (bankDay != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("O banco informou dia $bankDay.", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(day) }, enabled = day != null && day in 1..31) { Text("Salvar") } },
        dismissButton = {
            Row {
                if (bankDay != null) TextButton(onClick = { onSave(null) }) { Text("Usar o do banco") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

/** Bloco do início: fatura aberta de cada cartão cujo dia de fechamento é conhecido. */
@Composable
fun CardsHomeBlock(vm: AppViewModel, cards: List<Account>, allTxs: List<TxItem>, goCards: () -> Unit) {
    val today = LocalDate.now()
    NCard(Modifier.fillMaxWidth(), onClick = goCards) {
        SectionTitle("Faturas dos cartões")
        Spacer(Modifier.height(6.dp))
        val known = cards.mapNotNull { c -> vm.closingDayFor(c.id)?.let { c to it } }
        if (cards.isEmpty()) {
            Text("Nenhum cartão de crédito conectado.", style = MaterialTheme.typography.bodyMedium)
        } else if (known.isEmpty()) {
            Text("Informe o dia de fechamento de cada cartão em Planejar > Cartões para ver a fatura aberta aqui.",
                style = MaterialTheme.typography.bodyMedium)
        } else {
            known.forEach { (card, day) ->
                val cycle = cycleContaining(today, day)
                val s = summarizeCycle(allTxs.filter { it.tx.accountId == card.id }, cycle)
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(card.institution + " · " + card.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(statusText(cycle, today), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(money(s.total), fontWeight = FontWeight.Medium, color = if (s.total > 0) ExpenseColor else IncomeColor)
                }
            }
        }
        Text("Ver faturas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp))
    }
}
