package com.japaneixxx.freya.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class MonthSum(val label: String, val income: Double, val expense: Double)

@Composable
fun MonthlyBars(data: List<MonthSum>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val max = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0).coerceAtLeast(1.0)
    val progress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) { progress.animateTo(1f, tween(1000, easing = Motion.EaseOut)) }
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val p = progress.value
            val slot = size.width / data.size
            val bw = minOf(slot * 0.22f, 18.dp.toPx())
            val radius = CornerRadius(bw / 2f)
            val incomeBrush = Brush.verticalGradient(
                listOf(IncomeColor.copy(alpha = 0.55f), IncomeColor),
                startY = 0f,
                endY = size.height
            )
            val expenseBrush = Brush.verticalGradient(
                listOf(ExpenseColor.copy(alpha = 0.55f), ExpenseColor),
                startY = 0f,
                endY = size.height
            )
            data.forEachIndexed { i, d ->
                val cx = slot * i + slot / 2
                val hi = (d.income / max).toFloat() * size.height * p
                val he = (d.expense / max).toFloat() * size.height * p
                if (hi > 0.5f) {
                    drawRoundRect(
                        brush = incomeBrush,
                        topLeft = Offset(cx - bw - 2f, size.height - hi),
                        size = Size(bw, hi),
                        cornerRadius = radius
                    )
                }
                if (he > 0.5f) {
                    drawRoundRect(
                        brush = expenseBrush,
                        topLeft = Offset(cx + 2f, size.height - he),
                        size = Size(bw, he),
                        cornerRadius = radius
                    )
                }
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
fun DonutChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier, centerLabel: String = "Despesas") {
    val totalValue = data.sumOf { it.second }
    val total = totalValue.coerceAtLeast(1.0)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) { progress.animateTo(1f, tween(1000, easing = Motion.EaseOut)) }
    Box(modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 30f
            val d = size.minDimension - stroke
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            var start = -90f - 16f * (1f - progress.value)
            data.forEach { (cat, v) ->
                val sweep = (v / total * 360.0).toFloat() * progress.value
                if (sweep > 0.4f) {
                    drawArc(
                        categoryColor(cat), start + 1f, (sweep - 2f).coerceAtLeast(0.4f), false,
                        tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Butt)
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedMoney(totalValue, style = MaterialTheme.typography.titleMedium)
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

private fun DrawScope.endPoint(values: List<Float>, maxV: Float, totalDays: Int): Offset? {
    if (values.isEmpty()) return null
    val i = values.lastIndex
    val x = if (totalDays <= 1) 0f else size.width * i / (totalDays - 1)
    val y = size.height - (values.last() / maxV) * size.height
    return Offset(x, y)
}

/** Gasto acumulado dia a dia: mês atual (forte) contra mês anterior (suave). */
@Composable
fun CumulativeChart(cur: List<Float>, prev: List<Float>, totalDays: Int, modifier: Modifier = Modifier) {
    val maxV = maxOf(cur.maxOrNull() ?: 0f, prev.maxOrNull() ?: 0f, 1f)
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val progress = remember(cur, prev, totalDays) { Animatable(0f) }
    LaunchedEffect(cur, prev, totalDays) { progress.animateTo(1f, tween(1100, easing = Motion.EaseOut)) }
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val p = progress.value
        clipRect(right = size.width * p) {
            line(prev, maxV, totalDays, c2, 4f)
            line(cur, maxV, totalDays, c1, 6f)
        }
        val tip = endPoint(cur, maxV, totalDays)
        if (tip != null && p > 0.02f) {
            val visibleTip = Offset(tip.x.coerceAtMost(size.width * p), tip.y)
            drawCircle(c1.copy(alpha = 0.22f), radius = 11f, center = visibleTip)
            drawCircle(c1, radius = 5f, center = visibleTip)
        }
    }
}

/** Linha simples com os valores normalizados entre mínimo e máximo (ex.: evolução do saldo). */
@Composable
fun TrendChart(values: List<Double>, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    val lo = values.min(); val hi = values.max()
    val range = (hi - lo).coerceAtLeast(1.0)
    val color = MaterialTheme.colorScheme.primary
    val fill = Brush.verticalGradient(
        listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0.01f))
    )
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(1150, easing = Motion.EaseOut)) }
    val pulse = rememberInfiniteTransition(label = "trendDot")
    val dotRadius by pulse.animateFloat(
        initialValue = 4.5f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1500, easing = Motion.EaseInOut), RepeatMode.Reverse),
        label = "trendDotRadius"
    )
    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        val pts = values.mapIndexed { i, v ->
            Offset(size.width * i / (values.size - 1), (size.height - ((v - lo) / range).toFloat() * (size.height - 8f)) - 4f)
        }
        val area = Path().apply {
            moveTo(pts.first().x, size.height)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, size.height)
            close()
        }
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val p = progress.value
        clipRect(right = size.width * p) {
            drawPath(area, fill)
            drawPath(path, color, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (p > 0.02f) {
            val tip = pts.last()
            val visibleTip = Offset(tip.x.coerceAtMost(size.width * p), tip.y)
            drawCircle(color.copy(alpha = 0.20f), radius = dotRadius * 2.2f, center = visibleTip)
            drawCircle(color, radius = dotRadius, center = visibleTip)
        }
    }
}

/** Evolução de uma simulação de investimento: o que foi aportado (suave) contra o valor líquido (forte). */
@Composable
fun GrowthChart(invested: List<Double>, net: List<Double>, modifier: Modifier = Modifier) {
    if (net.size < 2) return
    val maxV = maxOf(invested.maxOrNull() ?: 0.0, net.maxOrNull() ?: 0.0).toFloat().coerceAtLeast(1f)
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val progress = remember(invested, net) { Animatable(0f) }
    LaunchedEffect(invested, net) { progress.animateTo(1f, tween(1100, easing = Motion.EaseOut)) }
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val p = progress.value
        clipRect(right = size.width * p) {
            line(invested.map { it.toFloat() }, maxV, invested.size, c2, 4f)
            line(net.map { it.toFloat() }, maxV, net.size, c1, 6f)
        }
        val tip = endPoint(net.map { it.toFloat() }, maxV, net.size)
        if (tip != null && p > 0.02f) {
            val visibleTip = Offset(tip.x.coerceAtMost(size.width * p), tip.y)
            drawCircle(c1.copy(alpha = 0.22f), radius = 11f, center = visibleTip)
            drawCircle(c1, radius = 5f, center = visibleTip)
        }
    }
}
