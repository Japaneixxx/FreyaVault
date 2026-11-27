package com.japaneixxx.freya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Um passo do tutorial. [target] é o elemento destacado (null = cartão no centro), [tab] a aba que precisa estar aberta e
 * [tapTab] marca um passo interativo: o usuário precisa tocar no destaque, que abre essa aba, para seguir.
 */
data class TourStep(
    val title: String,
    val text: String,
    val target: String? = null,
    val tab: Int? = null,
    val tapTab: Int? = null
)

val TOUR_STEPS = listOf(
    TourStep(
        "Bem-vindo ao Freya",
        "Vamos conhecer o app em menos de um minuto, tocando nas abas. Você pode pular quando quiser e rever depois em Mais > Ajuda.",
        tab = 0
    ),
    TourStep(
        "Seu saldo",
        "Aqui ficam o saldo das contas, a fatura dos cartões e a hora da última atualização. Sincronizar busca dados novos do banco, Ocultar esconde os valores e + Lançamento registra algo à mão.",
        target = "hero", tab = 0
    ),
    TourStep("Extrato", "Toque em Extrato para ver todos os seus lançamentos.", target = "nav_1", tapTab = 1),
    TourStep(
        "Seus lançamentos",
        "Busque por nome, categoria ou nota e filtre por mês, receitas, despesas ou categoria. Toque num lançamento para trocar a categoria ou anotar; segure para escolher vários de uma vez.",
        target = "extrato_search", tab = 1
    ),
    TourStep("Patrimônio", "Agora toque em Patrimônio.", target = "nav_2", tapTab = 2),
    TourStep(
        "Tudo o que você tem",
        "Soma contas, investimentos e bens e subtrai dívidas e faturas. Dá para cadastrar bens e dívidas, ver os investimentos e simular um investimento.",
        tab = 2
    ),
    TourStep("Planejar", "Toque em Planejar.", target = "nav_3", tapTab = 3),
    TourStep(
        "Organize o futuro",
        "Orçamentos, Metas, Compromissos (contas a pagar), Agenda dos próximos dias, Cartões com a fatura de cada ciclo, Parcelas, Recorrentes e Cobranças. Deslize as opções para o lado para ver todas.",
        target = "plan_tabs", tab = 3
    ),
    TourStep("Mais", "Por último, toque em Mais.", target = "nav_4", tapTab = 4),
    TourStep(
        "Ajustes do app",
        "Em Mais você renomeia contas, liga alertas, bloqueio por biometria ou PIN e os lembretes na agenda do celular, escolhe o tema, faz backup e confere as credenciais do Pluggy.",
        tab = 4
    ),
    TourStep("De volta ao início", "Toque em Início para terminar.", target = "nav_0", tapTab = 0),
    TourStep(
        "Deixe do seu jeito",
        "No fim da tela Início, toque em Personalizar início para escolher quais blocos aparecem e em que ordem. Pronto, é só isso!",
        tab = 0
    )
)

/** Guarda a posição na janela dos elementos que o tutorial pode destacar. */
class TourState {
    val targets = mutableStateMapOf<String, Rect>()
}

val LocalTour = compositionLocalOf<TourState?> { null }

/** Marca um elemento como possível destaque do tutorial. Sem tutorial ativo, não faz nada. */
@Composable
fun Modifier.tourTarget(id: String): Modifier {
    val tour = LocalTour.current ?: return this
    DisposableEffect(id) { onDispose { tour.targets.remove(id) } }
    return this.onGloballyPositioned { c ->
        val r = c.boundsInWindow()
        if (tour.targets[id] != r) tour.targets[id] = r
    }
}

private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) { detectTapGestures { } }

@Composable
fun TourOverlay(
    step: TourStep, index: Int, total: Int, state: TourState,
    onNext: () -> Unit, onSkip: () -> Unit
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val scrim = Color.Black.copy(alpha = 0.74f)
    val interactive = step.tapTab != null

    Box(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow(); size = it.size }
    ) {
        val pad = with(density) { 8.dp.toPx() }
        val hole = step.target?.let { state.targets[it] }
            ?.takeIf { size != IntSize.Zero }
            ?.translate(-origin.x, -origin.y)
            ?.inflate(pad)

        if (hole == null) {
            Box(Modifier.fillMaxSize().background(scrim).blockTouches())
        } else {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            @Composable
            fun Piece(l: Float, t: Float, r: Float, b: Float) {
                val width = (r - l).coerceAtLeast(0f)
                val height = (b - t).coerceAtLeast(0f)
                if (width <= 0f || height <= 0f) return
                Box(
                    Modifier.offset { IntOffset(l.toInt(), t.toInt()) }
                        .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                        .background(scrim).blockTouches()
                )
            }
            val top = hole.top.coerceIn(0f, h)
            val bottom = hole.bottom.coerceIn(0f, h)
            val left = hole.left.coerceIn(0f, w)
            val right = hole.right.coerceIn(0f, w)
            Piece(0f, 0f, w, top)
            Piece(0f, bottom, w, h)
            Piece(0f, top, left, bottom)
            Piece(right, top, w, bottom)
            // Passo informativo: o destaque aparece mas não recebe toques. Passo interativo: o toque passa para o app.
            if (!interactive) {
                Box(
                    Modifier.offset { IntOffset(left.toInt(), top.toInt()) }
                        .size(with(density) { (right - left).coerceAtLeast(0f).toDp() }, with(density) { (bottom - top).coerceAtLeast(0f).toDp() })
                        .blockTouches()
                )
            }
            Box(
                Modifier.offset { IntOffset(left.toInt(), top.toInt()) }
                    .size(with(density) { (right - left).coerceAtLeast(0f).toDp() }, with(density) { (bottom - top).coerceAtLeast(0f).toDp() })
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            )
        }

        val holeCenterY = hole?.center?.y
        val cardAtTop = holeCenterY != null && holeCenterY > size.height / 2f
        val align = when {
            holeCenterY == null -> Alignment.Center
            cardAtTop -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }
        Box(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), contentAlignment = align) {
            Card(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("${index + 1} de $total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(step.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(step.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onSkip) { Text("Pular tutorial") }
                        Spacer(Modifier.weight(1f))
                        if (interactive) {
                            Text("Toque no destaque", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(onClick = onNext) { Text(if (index == total - 1) "Concluir" else "Próximo") }
                        }
                    }
                }
            }
        }
    }
}
