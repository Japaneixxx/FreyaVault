package app.meufin.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class MonthSum(val label: String, val income: Double, val expense: Double)

@Composable
fun MonthlyBars(data: List<MonthSum>, modifier: Modifier = Modifier) {
    val max = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0).coerceAtLeast(1.0)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val slot = size.width / data.size
            val bw = slot * 0.22f
            data.forEachIndexed { i, d ->
                val cx = slot * i + slot / 2
                val hi = (d.income / max).toFloat() * size.height
                val he = (d.expense / max).toFloat() * size.height
                drawRoundRect(IncomeColor, Offset(cx - bw - 3f, size.height - hi), Size(bw, hi), CornerRadius(10f))
                drawRoundRect(ExpenseColor, Offset(cx + 3f, size.height - he), Size(bw, he), CornerRadius(10f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            data.forEach {
                Text(
                    it.label, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DonutChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.second }.coerceAtLeast(1.0)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 30f
            val d = size.minDimension - stroke
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            var start = -90f
            data.forEach { (cat, v) ->
                val sweep = (v / total * 360.0).toFloat()
                drawArc(
                    categoryColor(cat), start + 1f, (sweep - 2f).coerceAtLeast(0.5f), false,
                    tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Despesas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(money(data.sumOf { it.second }), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun DrawScope.line(values: List<Float>, maxV: Float, totalDays: Int, color: Color, width: Float) {
    if (values.isEmpty()) return
    val path = Path()
    values.forEachIndexed { i, v ->
        val x = if (totalDays <= 1) 0f else size.width * i / (totalDays - 1)
        val y = size.height - (v / maxV) * size.height
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Gasto acumulado dia a dia: mês atual (forte) contra mês anterior (suave). */
@Composable
fun CumulativeChart(cur: List<Float>, prev: List<Float>, totalDays: Int, modifier: Modifier = Modifier) {
    val maxV = maxOf(cur.maxOrNull() ?: 0f, prev.maxOrNull() ?: 0f, 1f)
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        line(prev, maxV, totalDays, c2, 4f)
        line(cur, maxV, totalDays, c1, 6f)
    }
}
