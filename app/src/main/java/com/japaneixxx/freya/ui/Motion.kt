package com.japaneixxx.freya.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Ritmo visual único do app: durações e curvas compartilhadas. */
object Motion {
    const val Fast = 200
    const val Medium = 480
    const val Slow = 900

    /** Curva suave de saída, boa para entradas e números. */
    val EaseOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Curva simétrica para brilhos e pulsos. */
    val EaseInOut: Easing = FastOutSlowInEasing
}

/**
 * Anima um número de 0 até [target] (e entre valores seguintes) com contagem suave.
 * Usado para saldos, totais e percentuais.
 */
@Composable
fun rememberAnimatedDouble(
    target: Double,
    durationMillis: Int = Motion.Slow,
    delayMillis: Int = 0
): Double {
    val anim = remember { Animatable(0f) }
    var from by remember { mutableStateOf(0.0) }
    var to by remember { mutableStateOf(0.0) }
    LaunchedEffect(target, durationMillis, delayMillis) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        // Continua a partir do valor visível, mesmo se o alvo mudar no meio do caminho.
        from = from + (to - from) * anim.value.toDouble()
        to = target
        anim.snapTo(0f)
        anim.animateTo(1f, tween(durationMillis, easing = Motion.EaseOut))
    }
    return if (anim.value >= 1f) to else from + (to - from) * anim.value.toDouble()
}

/** Texto monetário com contagem animada. */
@Composable
fun AnimatedMoney(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    durationMillis: Int = Motion.Slow
) {
    val animated = rememberAnimatedDouble(value, durationMillis)
    Text(
        text = money(animated),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

/** Toque com leve compressão elástica, mantendo o ripple do Material. */
fun Modifier.springClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.972f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 750f),
        label = "springClick"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interaction,
        indication = LocalIndication.current,
        enabled = enabled,
        onClick = onClick
    )
}

/** Entrada em cascata para cartões e linhas de uma lista. */
fun Modifier.appear(index: Int = 0, durationMillis: Int = Motion.Medium): Modifier = composed {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceIn(0, 12) * 42).toLong())
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis, easing = Motion.EaseOut),
        label = "appear"
    )
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 22.dp.toPx()
        scaleX = 0.985f + 0.015f * progress
        scaleY = 0.985f + 0.015f * progress
    }
}

/** Pulso suave e contínuo, para avisos e ícones que pedem atenção. */
fun Modifier.pulse(
    minScale: Float = 0.94f,
    maxScale: Float = 1.04f,
    periodMillis: Int = 1600
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = Motion.EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
