package com.japaneixxx.freya.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

// Paleta nórdica: névoa, fiorde, pinho, areia.
val IncomeColor = Color(0xFF4F8F7A)
val ExpenseColor = Color(0xFFC4605F)
val WarnColor = Color(0xFFD4A373)

private val palette = listOf(
    Color(0xFF5B7FA6), Color(0xFF8FB4A3), Color(0xFFD4A373), Color(0xFFB07A8A),
    Color(0xFF7C8DA3), Color(0xFFC9B8A0), Color(0xFF6E9C9A), Color(0xFFA3A9C4),
    Color(0xFFCC8F7E), Color(0xFF94A88B)
)

fun categoryColor(name: String): Color = palette[(name.hashCode() and 0x7fffffff) % palette.size]

private val LightScheme = lightColorScheme(
    primary = Color(0xFF3F5F8A), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE7F2), onPrimaryContainer = Color(0xFF1B2A3C),
    secondary = Color(0xFF6E9C9A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEEC), onSecondaryContainer = Color(0xFF1E3533),
    background = Color(0xFFF4F6F8), onBackground = Color(0xFF1F2933),
    surface = Color.White, onSurface = Color(0xFF1F2933),
    surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = Color(0xFF5B6773),
    outline = Color(0xFFB7C0CA), outlineVariant = Color(0xFFDDE3E9),
    error = Color(0xFFC4605F)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9DBBE0), onPrimary = Color(0xFF14202F),
    primaryContainer = Color(0xFF243447), onPrimaryContainer = Color(0xFFDDE7F2),
    secondary = Color(0xFF8FB4B2), onSecondary = Color(0xFF14211F),
    secondaryContainer = Color(0xFF263635), onSecondaryContainer = Color(0xFFE3EEEC),
    background = Color(0xFF11171E), onBackground = Color(0xFFE6EBF0),
    surface = Color(0xFF19212A), onSurface = Color(0xFFE6EBF0),
    surfaceVariant = Color(0xFF232D39), onSurfaceVariant = Color(0xFF9AA7B4),
    outline = Color(0xFF5B6773), outlineVariant = Color(0xFF2B3744),
    error = Color(0xFFE08583)
)

private val NordicTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-1).sp),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Light),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Light),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Normal),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = t.labelLarge.copy(letterSpacing = 0.4.sp)
    )
}

private val NordicShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AppTheme(mode: Int = 0, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val colorScheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NordicTypography,
        shapes = NordicShapes,
        content = content
    )
}

/** Cartão nórdico: borda fina, cantos generosos e resposta elástica ao toque. */
@Composable
fun NCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surface,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) modifier.springClickable(onClick = onClick) else modifier
    Card(
        modifier = clickModifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f))
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
