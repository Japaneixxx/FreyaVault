package com.japaneixxx.freya.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.japaneixxx.freya.data.ALL_CATEGORIES
import com.japaneixxx.freya.data.BUDGET_CATEGORIES
import com.japaneixxx.freya.data.CAT_CARD_PAYMENT
import com.japaneixxx.freya.data.CAT_OWN_TRANSFER
import com.japaneixxx.freya.data.TxKind
import java.time.YearMonth

/** Categorias padrão + as criadas pelo usuário. */
object Cats {
    var custom by mutableStateOf<List<String>>(emptyList())
    val all: List<String> get() = ALL_CATEGORIES + custom
    val budget: List<String> get() = BUDGET_CATEGORIES + custom
}

private val categoryEmoji = mapOf(
    "Mercado" to "🛒", "Restaurantes" to "🍽️", "Delivery" to "🛵", "Transporte" to "🚗",
    "Moradia" to "🏠", "Saúde" to "💊", "Lazer" to "🎮", "Assinaturas" to "📺",
    "Compras" to "🛍️", "Educação" to "🎓", "Viagem" to "✈️", "Impostos e taxas" to "🧾",
    "Investimentos" to "📈", "Receitas" to "💰", "Transferências" to "🔁",
    CAT_CARD_PAYMENT to "💳", CAT_OWN_TRANSFER to "↔️", "Outros" to "🔹", "Todas" to "🗂️",
    "Renda fixa" to "🏛️", "Ações" to "📊", "Fundos" to "🧺", "ETFs" to "📦", "COE" to "📑",
    "Previdência e títulos" to "🌴", "Contas" to "🏦", "Bens" to "🏡", "Dívidas" to "💸", "Cartões" to "💳"
)

fun emojiFor(category: String): String = categoryEmoji[category] ?: "🏷️"

@Composable
fun CategoryAvatar(category: String, size: Dp = 42.dp) {
    Box(
        Modifier.size(size).background(categoryColor(category).copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(emojiFor(category), fontSize = (size.value * 0.44f).sp) }
}

/** Barra de progresso arredondada e animada. */
@Composable
fun NProgress(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "progress")
    Box(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(RoundedCornerShape(50)).background(color))
    }
}

/** Etiqueta pequena (pendente, manual, ignorada...). */
@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Chip que abre um menu de opções (filtros). */
@Composable
fun ChipPicker(label: String, active: Boolean, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = active, onClick = { open = true },
            label = { Text(label, maxLines = 1) },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)) }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); open = false }) }
        }
    }
}

/** Cria uma categoria nova a partir do seletor (fornecido pelo App). */
val LocalCategoryCreator = staticCompositionLocalOf<(String) -> Unit> { {} }

/** Categorias mais usadas, mostradas no topo do seletor. */
val LocalRecentCategories = staticCompositionLocalOf<List<String>> { emptyList() }

@Composable
private fun CategoryTile(name: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CategoryAvatar(name, 46.dp)
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
    }
}

/** Seletor de categorias em grade: busca, mais usadas e criação de categoria na hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySheet(
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    allowCreate: Boolean = true
) {
    val creator = LocalCategoryCreator.current
    val recents = LocalRecentCategories.current.filter { it in options }.take(5)
    var query by remember { mutableStateOf("") }
    val filtered = options.filter { query.isBlank() || it.contains(query, ignoreCase = true) }
    val canCreate = allowCreate && query.isNotBlank() && options.none { it.equals(query.trim(), ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Categoria", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            TextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Buscar ou criar categoria") },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isBlank() && recents.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Mais usadas")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recents.forEach { c ->
                        FilterChip(
                            selected = c == selected,
                            onClick = { onSelect(c); onDismiss() },
                            label = { Text("${emojiFor(c)}  $c") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it }) { c -> CategoryTile(c, c == selected) { onSelect(c); onDismiss() } }
                if (canCreate) item(key = "create") {
                    Column(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .clickable {
                                val name = query.trim()
                                creator(name)
                                onSelect(name)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.height(6.dp))
                        Text("Criar \"${query.trim()}\"", style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
                    }
                }
            }
            if (filtered.isEmpty() && !canCreate) {
                Text("Nenhuma categoria encontrada.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

/** Botão que mostra a categoria atual e abre o seletor em grade. */
@Composable
fun CategoryPicker(
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text(emojiFor(selected))
        Spacer(Modifier.width(8.dp))
        Text(selected)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
    }
    if (open) CategorySheet(selected, options, onSelect, { open = false })
}

/** Chip de filtro que abre o seletor de categorias (sem criação de categoria). */
@Composable
fun CategoryFilterChip(label: String, active: Boolean, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FilterChip(
        selected = active, onClick = { open = true },
        label = { Text(label, maxLines = 1) },
        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)) }
    )
    if (open) CategorySheet(if (active) label else null, options, onSelect, { open = false }, allowCreate = false)
}

/** Sinalizações do lançamento: tipo (Pix, débito, fatura...) e parcela. */
@Composable
fun KindPills(kind: TxKind?, parcel: String?) {
    if (kind != null) Pill("${kind.emoji} ${kind.label}")
    if (parcel != null) Pill("🧩 $parcel")
}

fun parseAmount(text: String): Double? = text.trim().replace(",", ".").toDoubleOrNull()

/** Montanhas em camadas: assinatura visual nórdica do topo do Início. */
@Composable
fun Mountains(modifier: Modifier = Modifier) {
    val c1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val c2 = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val c3 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
    Canvas(modifier.fillMaxWidth().height(84.dp)) {
        fun ridge(color: Color, peaks: List<Pair<Float, Float>>) {
            val p = Path()
            p.moveTo(0f, size.height)
            peaks.forEach { (x, y) -> p.lineTo(size.width * x, size.height * y) }
            p.lineTo(size.width, size.height)
            p.close()
            drawPath(p, color)
        }
        ridge(c1, listOf(0f to 0.55f, 0.18f to 0.22f, 0.38f to 0.6f, 0.6f to 0.12f, 0.82f to 0.5f, 1f to 0.3f))
        ridge(c2, listOf(0f to 0.8f, 0.25f to 0.48f, 0.5f to 0.85f, 0.75f to 0.42f, 1f to 0.75f))
        ridge(c3, listOf(0f to 1f, 0.3f to 0.78f, 0.6f to 1f, 0.85f to 0.84f, 1f to 1f))
    }
}

/** Calendário do mês: cada dia fica mais escuro quanto maior o gasto. */
@Composable
fun SpendHeatmap(ym: YearMonth, perDay: Map<Int, Double>, modifier: Modifier = Modifier) {
    val max = (perDay.values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val lead = ym.atDay(1).dayOfWeek.value % 7 // domingo = 0
    val cells = List(lead) { 0 } + (1..ym.lengthOfMonth()).toList()
    val padded = cells + List((7 - cells.size % 7) % 7) { 0 }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        padded.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    if (day == 0) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val v = perDay[day] ?: 0.0
                        val bg = if (v <= 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + 0.62f * (v / max).toFloat())
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).background(bg, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$day", style = MaterialTheme.typography.labelSmall,
                                color = if (v / max > 0.55) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
