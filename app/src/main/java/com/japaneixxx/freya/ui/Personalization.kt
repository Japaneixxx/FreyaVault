package com.japaneixxx.freya.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.abs

// ---------------- Cor de destaque ----------------
/** Cores derivadas da cor de destaque escolhida, já ajustadas para ter contraste no tema claro ou escuro (ARGB). */
data class AccentColors(
    val primary: Int, val onPrimary: Int,
    val primaryContainer: Int, val onPrimaryContainer: Int,
    val secondary: Int, val onSecondary: Int,
    val secondaryContainer: Int, val onSecondaryContainer: Int
)

/** ARGB -> [matiz 0..360, saturação 0..1, luminosidade 0..1]. */
fun argbToHsl(argb: Int): FloatArray {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    if (d == 0f) return floatArrayOf(0f, 0f, l)
    val s = d / (1f - abs(2f * l - 1f))
    val h = when (max) {
        r -> 60f * (((g - b) / d) % 6f)
        g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return floatArrayOf(if (h < 0f) h + 360f else h, s.coerceIn(0f, 1f), l)
}

fun hslToArgb(hue: Float, sat: Float, light: Float): Int {
    val h = ((hue % 360f) + 360f) % 360f
    val s = sat.coerceIn(0f, 1f)
    val l = light.coerceIn(0f, 1f)
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun ch(v: Float) = ((v + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
}

private fun channelLuminance(v: Int): Double {
    val c = v / 255.0
    return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
}

private fun luminance(argb: Int): Double =
    0.2126 * channelLuminance((argb shr 16) and 0xFF) + 0.7152 * channelLuminance((argb shr 8) and 0xFF) + 0.0722 * channelLuminance(argb and 0xFF)

/** Razão de contraste (WCAG) entre duas cores; 4,5 ou mais é boa para texto. */
fun contrastRatio(a: Int, b: Int): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

private const val MIN_CONTRAST = 4.5
private const val WHITE = 0xFFFFFFFF.toInt()

/** Escurece até o texto branco ficar legível sobre a cor (amarelos e limões precisam disso). */
private fun forWhiteText(h: Float, s: Float, l: Float): Int {
    var light = l
    var c = hslToArgb(h, s, light)
    while (contrastRatio(c, WHITE) < MIN_CONTRAST && light > 0.12f) { light -= 0.02f; c = hslToArgb(h, s, light) }
    return c
}

/** Clareia até o texto escuro [text] ficar legível sobre a cor. */
private fun forDarkText(h: Float, s: Float, l: Float, text: Int): Int {
    var light = l
    var c = hslToArgb(h, s, light)
    while (contrastRatio(c, text) < MIN_CONTRAST && light < 0.95f) { light += 0.02f; c = hslToArgb(h, s, light) }
    return c
}

/** Deriva o esquema do app a partir de uma cor: mantém o matiz e a saturação escolhidos e ajusta só a luminosidade. */
fun accentColors(argb: Int, dark: Boolean): AccentColors {
    val (h, s0, l0) = argbToHsl(argb).let { Triple(it[0], it[1], it[2]) }
    val s = s0.coerceAtMost(0.9f)
    val h2 = h + 35f // secundária: vizinha no círculo de cores, para o degradê do início ter vida
    return if (dark) {
        val onPrimary = hslToArgb(h, s * 0.5f, 0.08f)
        val onSecondary = hslToArgb(h2, s * 0.4f, 0.08f)
        AccentColors(
            primary = forDarkText(h, s, l0.coerceIn(0.64f, 0.80f), onPrimary), onPrimary = onPrimary,
            primaryContainer = hslToArgb(h, s * 0.45f, 0.22f), onPrimaryContainer = hslToArgb(h, s * 0.5f, 0.90f),
            secondary = forDarkText(h2, s * 0.45f, 0.72f, onSecondary), onSecondary = onSecondary,
            secondaryContainer = hslToArgb(h2, s * 0.30f, 0.22f), onSecondaryContainer = hslToArgb(h2, s * 0.4f, 0.90f)
        )
    } else {
        AccentColors(
            primary = forWhiteText(h, s, l0.coerceIn(0.30f, 0.46f)), onPrimary = WHITE,
            primaryContainer = hslToArgb(h, s * 0.55f, 0.91f), onPrimaryContainer = hslToArgb(h, s * 0.6f, 0.15f),
            secondary = forWhiteText(h2, s * 0.5f, 0.42f), onSecondary = WHITE,
            secondaryContainer = hslToArgb(h2, s * 0.40f, 0.91f), onSecondaryContainer = hslToArgb(h2, s * 0.5f, 0.16f)
        )
    }
}

data class AccentPreset(val name: String, val argb: Int)

/** 0 = cor padrão do Freya (fiorde). Vermelho fica de fora de propósito: no app ele significa gasto. */
val ACCENT_PRESETS = listOf(
    AccentPreset("Fiorde", 0),
    AccentPreset("Pinho", 0xFF3F7D6B.toInt()),
    AccentPreset("Turquesa", 0xFF2E8B95.toInt()),
    AccentPreset("Índigo", 0xFF4455B8.toInt()),
    AccentPreset("Aurora", 0xFF7A5FA6.toInt()),
    AccentPreset("Rosa", 0xFFB0587A.toInt()),
    AccentPreset("Âmbar", 0xFFB7791F.toInt()),
    AccentPreset("Oliva", 0xFF7A8B3A.toInt()),
    AccentPreset("Ardósia", 0xFF5A6B7D.toInt())
)

// ---------------- Fonte e densidade ----------------
const val FONT_PCT_MIN = 80
const val FONT_PCT_MAX = 140
val DENSITY_LABELS = listOf("Compacto", "Padrão", "Confortável")

/** Fator aplicado ao tamanho de tudo que é medido em dp (espaços, cartões, ícones). O texto não muda com a densidade. */
fun densityFactor(level: Int): Float = when (level) { 0 -> 0.9f; 2 -> 1.1f; else -> 1f }

/** Aplica o tamanho do texto e a densidade escolhidos a tudo que estiver dentro. */
@Composable
fun PersonalizedDensity(fontScalePct: Int, densityLevel: Int, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val m = densityFactor(densityLevel)
    val font = fontScalePct.coerceIn(FONT_PCT_MIN, FONT_PCT_MAX) / 100f
    // O texto mede sp * fontScale * density: dividir a escala pela densidade mantém o texto no tamanho pedido.
    val d = Density(base.density * m, base.fontScale * font / m)
    CompositionLocalProvider(LocalDensity provides d, content = content)
}

// ---------------- Abas ----------------
/** Rótulos pelo número fixo de cada aba: 0 Início, 1 Extrato, 2 Patrimônio, 3 Planejar, 4 Mais. */
val TAB_LABELS = listOf("Início", "Extrato", "Patrimônio", "Planejar", "Mais")

/** Lê "0,2,1,3,4". Sempre devolve as 5 abas, sem repetir, completando o que faltar no fim. */
fun parseTabOrder(raw: String): List<Int> {
    val seen = mutableSetOf<Int>()
    val out = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in TAB_LABELS.indices && seen.add(it) }
    return out + TAB_LABELS.indices.filter { it !in seen }
}

fun encodeTabOrder(order: List<Int>): String = order.joinToString(",")

fun moveTab(order: List<Int>, index: Int, delta: Int): List<Int> {
    val to = index + delta
    if (index !in order.indices || to !in order.indices) return order
    return order.toMutableList().also { val a = it[index]; it[index] = it[to]; it[to] = a }
}

// ---------------- Atalhos e destinos ----------------
/** Para onde um atalho, o widget ou uma notificação leva: aba, seção do Planejar e uma ação opcional ("add", "sync"). */
data class DeepLink(val tab: Int, val planSection: Int? = null, val action: String? = null)

data class ShortcutDef(val id: String, val label: String, val link: DeepLink)

val SHORTCUTS = listOf(
    ShortcutDef("add", "Novo lançamento", DeepLink(0, action = "add")),
    ShortcutDef("agenda", "Agenda", DeepLink(3, PlanSection.AGENDA)),
    ShortcutDef("cartoes", "Faturas dos cartões", DeepLink(3, PlanSection.CARDS)),
    ShortcutDef("orcamentos", "Orçamentos", DeepLink(3, PlanSection.BUDGETS)),
    ShortcutDef("extrato", "Extrato", DeepLink(1)),
    ShortcutDef("patrimonio", "Patrimônio", DeepLink(2)),
    ShortcutDef("sync", "Sincronizar agora", DeepLink(0, action = "sync"))
)

const val MAX_SHORTCUTS = 4
const val DEFAULT_SHORTCUTS = "add,agenda"

/** Destinos que o widget pode abrir ao toque: o início mais todos os atalhos. */
val LINK_TARGETS = listOf(ShortcutDef("home", "Início", DeepLink(0))) + SHORTCUTS

fun linkTarget(id: String): ShortcutDef = LINK_TARGETS.firstOrNull { it.id == id } ?: LINK_TARGETS.first()

fun parseShortcutIds(raw: String): List<String> {
    val known = SHORTCUTS.map { it.id }.toSet()
    return raw.split(",").map { it.trim() }.filter { it in known }.distinct().take(MAX_SHORTCUTS)
}
