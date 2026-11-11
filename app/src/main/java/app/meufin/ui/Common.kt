package app.meufin.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import app.meufin.data.ALL_CATEGORIES
import app.meufin.data.BUDGET_CATEGORIES
import app.meufin.data.CAT_CARD_PAYMENT
import app.meufin.data.CAT_OWN_TRANSFER
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
    CAT_CARD_PAYMENT to "💳", CAT_OWN_TRANSFER to "↔️", "Outros" to "🔹"
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

/** Botão que abre um menu com a lista de categorias. */
@Composable
fun CategoryPicker(
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) { Text(selected) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Text(emojiFor(it)); Spacer(Modifier.width(8.dp)); Text(it) } },
                    onClick = { onSelect(it); open = false }
                )
            }
        }
    }
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
