package com.japaneixxx.freya.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.roundToInt

data class MonthSum(val label: String, val income: Double, val expense: Double)

private fun Modifier.chartTouch(key: Any?, onTouch: (Offset, Size) -> Unit): Modifier = pointerInput(key) {
    val chartSize = Size(size.width.toFloat(), size.height.toFloat())
    if (chartSize.width <= 0f || chartSize.height <= 0f) return@pointerInput

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onTouch(down.position, chartSize)
        var change = down
        while (change.pressed) {
            val event = awaitPointerEvent()
            change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.pressed) onTouch(change.position, chartSize)
        }
    }
}

@Composable
private fun ChartDetail(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun ChartHint(text: String = "Toque ou arraste para consultar os valores.") {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun MonthlyBars(data: List<MonthSum>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val max = (data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0).coerceAtLeast(1.0)
    var selectedIndex by remember(data) { mutableIntStateOf(-1) }
    val selected = data.getOrNull(selectedIndex)
    val progress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) { progress.animateTo(1f, tween(1000, easing = Motion.EaseOut)) }
    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(130.dp).chartTouch(data) { position, canvasSize ->
                selectedIndex = (position.x / canvasSize.width * data.size).toInt().coerceIn(0, data.lastIndex)
            }
        ) {
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
        selected?.let { month ->
            ChartDetail {
                Text(month.label, style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Entradas", style = MaterialTheme.typography.labelSmall, color = IncomeColor)
                        Text(money(month.income), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Saídas", style = MaterialTheme.typography.labelSmall, color = ExpenseColor)
                        Text(money(month.expense), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } ?: ChartHint()
    }
}

@Composable
fun DonutChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier, centerLabel: String = "Despesas") {
    val totalValue = data.sumOf { it.second }
    val total = totalValue.coerceAtLeast(1.0)
    var selectedIndex by remember(data) { mutableIntStateOf(-1) }
    val selected = data.getOrNull(selectedIndex)
    val track = MaterialTheme.colorScheme.surfaceVariant
    val progress = remember(data) { Animatable(0f) }
    LaunchedEffect(data) { progress.animateTo(1f, tween(1000, easing = Motion.EaseOut)) }
    Box(
        modifier.size(170.dp).chartTouch(data) { position, canvasSize ->
            selectedIndex = selectedDonutSlice(data, position, canvasSize)
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 30f
            val d = size.minDimension - stroke
            val tl = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(stroke))
            var start = -90f - 16f * (1f - progress.value)
            data.forEachIndexed { index, (cat, v) ->
                val sweep = (v / total * 360.0).toFloat() * progress.value
                if (sweep > 0.4f) {
                    drawArc(
                        categoryColor(cat), start + 1f, (sweep - 2f).coerceAtLeast(0.4f), false,
                        tl, Size(d, d),
                        style = Stroke(stroke + if (index == selectedIndex) 5f else 0f, cap = StrokeCap.Butt)
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                selected?.first ?: centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected == null) {
                AnimatedMoney(totalValue, style = MaterialTheme.typography.titleMedium)
                ChartHint("Toque em uma fatia")
            } else {
                Text(
                    money(selected.second),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${(selected.second / totalValue.coerceAtLeast(0.01) * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun selectedDonutSlice(data: List<Pair<String, Double>>, position: Offset, size: Size): Int {
    val stroke = 30f
    val dx = position.x - size.width / 2f
    val dy = position.y - size.height / 2f
    val radiusSquared = dx * dx + dy * dy
    val arcRadius = (size.minDimension - stroke) / 2f
    if (radiusSquared < (arcRadius - stroke / 2f) * (arcRadius - stroke / 2f) ||
        radiusSquared > (arcRadius + stroke / 2f) * (arcRadius + stroke / 2f)
    ) return -1

    val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val angle = ((degrees + 90f) % 360f + 360f) % 360f
    val total = data.sumOf { it.second }.coerceAtLeast(1.0)
    var end = 0f
    data.forEachIndexed { index, (_, value) ->
        end += (value / total * 360.0).toFloat()
        if (angle < end) return index
    }
    return -1
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
fun CumulativeChart(
    cur: List<Float>,
    prev: List<Float>,
    totalDays: Int,
    modifier: Modifier = Modifier,
    currentLabel: String = "Este mês",
    previousLabel: String = "Mês anterior"
) {
    val maxV = maxOf(cur.maxOrNull() ?: 0f, prev.maxOrNull() ?: 0f, 1f)
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val guide = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    var selectedDay by remember(cur, prev, totalDays) { mutableIntStateOf(-1) }
    val progress = remember(cur, prev, totalDays) { Animatable(0f) }
    LaunchedEffect(cur, prev, totalDays) { progress.animateTo(1f, tween(1100, easing = Motion.EaseOut)) }
    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(120.dp).chartTouch(cur to prev) { position, canvasSize ->
                selectedDay = ((position.x / canvasSize.width).coerceIn(0f, 1f) * (totalDays - 1).coerceAtLeast(0)).roundToInt()
            }
        ) {
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
            if (selectedDay in 0 until totalDays && p >= 0.02f) {
                val x = if (totalDays <= 1) 0f else size.width * selectedDay / (totalDays - 1)
                drawLine(guide, Offset(x, 0f), Offset(x, size.height), 1f)
                cur.getOrNull(selectedDay)?.let { value ->
                    val y = size.height - (value / maxV) * size.height
                    drawCircle(c1, radius = 6f, center = Offset(x, y))
                }
                prev.getOrNull(selectedDay)?.let { value ->
                    val y = size.height - (value / maxV) * size.height
                    drawCircle(c2, radius = 4f, center = Offset(x, y))
                }
            }
        }
        if (selectedDay >= 0) {
            ChartDetail {
                Text("Dia ${selectedDay + 1}", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(currentLabel, style = MaterialTheme.typography.labelSmall, color = c1)
                        Text(cur.getOrNull(selectedDay)?.let { money(it.toDouble()) } ?: "Sem dados",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(previousLabel, style = MaterialTheme.typography.labelSmall, color = c2)
                        Text(prev.getOrNull(selectedDay)?.let { money(it.toDouble()) } ?: "Sem dados",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            ChartHint()
        }
    }
}

/** Linha simples com os valores normalizados entre mínimo e máximo (ex.: evolução do saldo). */
@Composable
fun TrendChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    valueLabel: String = "Saldo"
) {
    if (values.size < 2) return
    val lo = values.min(); val hi = values.max()
    val range = (hi - lo).coerceAtLeast(1.0)
    val color = MaterialTheme.colorScheme.primary
    val guide = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    var selectedIndex by remember(values, labels) { mutableIntStateOf(-1) }
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
    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(110.dp).chartTouch(values to labels) { position, canvasSize ->
                selectedIndex = ((position.x / canvasSize.width).coerceIn(0f, 1f) * values.lastIndex).roundToInt()
            }
        ) {
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
            if (selectedIndex in values.indices) {
                val point = pts[selectedIndex]
                drawLine(guide, Offset(point.x, 0f), Offset(point.x, size.height), 1f)
                drawCircle(color.copy(alpha = 0.22f), radius = 11f, center = point)
                drawCircle(color, radius = 5f, center = point)
            }
        }
        if (selectedIndex in values.indices) {
            ChartDetail {
                Text(labels.getOrNull(selectedIndex) ?: "Ponto ${selectedIndex + 1}", style = MaterialTheme.typography.titleSmall)
                Text("$valueLabel: ${money(values[selectedIndex])}", style = MaterialTheme.typography.bodyMedium, color = color)
            }
        } else {
            ChartHint()
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
    val guide = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    var selectedIndex by remember(invested, net) { mutableIntStateOf(-1) }
    val progress = remember(invested, net) { Animatable(0f) }
    LaunchedEffect(invested, net) { progress.animateTo(1f, tween(1100, easing = Motion.EaseOut)) }
    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(140.dp).chartTouch(invested to net) { position, canvasSize ->
                selectedIndex = ((position.x / canvasSize.width).coerceIn(0f, 1f) * net.lastIndex).roundToInt()
            }
        ) {
            val p = progress.value
            clipRect(right = size.width * p) {
                line(invested.map { it.toFloat() }, maxV, net.size, c2, 4f)
                line(net.map { it.toFloat() }, maxV, net.size, c1, 6f)
            }
            val tip = endPoint(net.map { it.toFloat() }, maxV, net.size)
            if (tip != null && p > 0.02f) {
                val visibleTip = Offset(tip.x.coerceAtMost(size.width * p), tip.y)
                drawCircle(c1.copy(alpha = 0.22f), radius = 11f, center = visibleTip)
                drawCircle(c1, radius = 5f, center = visibleTip)
            }
            if (selectedIndex in net.indices) {
                val x = size.width * selectedIndex / net.lastIndex
                drawLine(guide, Offset(x, 0f), Offset(x, size.height), 1f)
                val netY = size.height - (net[selectedIndex].toFloat() / maxV) * size.height
                drawCircle(c1.copy(alpha = 0.22f), radius = 11f, center = Offset(x, netY))
                drawCircle(c1, radius = 5f, center = Offset(x, netY))
                invested.getOrNull(selectedIndex)?.let { value ->
                    val investedY = size.height - (value.toFloat() / maxV) * size.height
                    drawCircle(c2, radius = 4f, center = Offset(x, investedY))
                }
            }
        }
        if (selectedIndex in net.indices) {
            val contribution = invested.getOrNull(selectedIndex) ?: 0.0
            val value = net[selectedIndex]
            ChartDetail {
                Text(
                    if (selectedIndex == 0) "Hoje" else "Após $selectedIndex ${if (selectedIndex == 1) "mês" else "meses"}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text("Aportado: ${money(contribution)}", style = MaterialTheme.typography.bodySmall, color = c2)
                Text("Valor líquido: ${money(value)}", style = MaterialTheme.typography.bodySmall, color = c1)
                Text("Rendimento líquido: ${money(value - contribution)}", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            ChartHint()
        }
    }
}
