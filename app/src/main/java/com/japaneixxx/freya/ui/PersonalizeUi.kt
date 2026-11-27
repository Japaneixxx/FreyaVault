package com.japaneixxx.freya.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Mais > Aparência: tema, cor de destaque, tamanho do texto e densidade. */
@Composable
fun AppearanceCard(vm: AppViewModel) {
    NCard(Modifier.fillMaxWidth()) {
        Text("Tema", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sistema", "Claro", "Escuro").forEachIndexed { i, l ->
                FilterChip(selected = vm.themeMode == i, onClick = { vm.setTheme(i) }, label = { Text(l) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Cor de destaque", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        AccentPicker(vm)

        Spacer(Modifier.height(16.dp))
        Text("Tamanho do texto", style = MaterialTheme.typography.titleSmall)
        var pct by remember(vm.fontScalePct) { mutableFloatStateOf(vm.fontScalePct.toFloat()) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = pct, onValueChange = { pct = it },
                onValueChangeFinished = { vm.updateFontScalePct(pct.toInt()) },
                valueRange = FONT_PCT_MIN.toFloat()..FONT_PCT_MAX.toFloat(),
                steps = (FONT_PCT_MAX - FONT_PCT_MIN) / 5 - 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text("${pct.toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        Text("Amostra: ${moneyPlain(1234.56)} no mercado", style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { vm.updateFontScalePct(100) }, enabled = vm.fontScalePct != 100) { Text("Voltar ao padrão") }

        Spacer(Modifier.height(8.dp))
        Text("Densidade", style = MaterialTheme.typography.titleSmall)
        Text(
            "Muda o espaço e o tamanho dos cartões e ícones. O texto segue o tamanho escolhido acima.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DENSITY_LABELS.forEachIndexed { i, l ->
                FilterChip(selected = vm.uiDensity == i, onClick = { vm.updateUiDensity(i) }, label = { Text(l) })
            }
        }
    }
}

@Composable
private fun AccentPicker(vm: AppViewModel) {
    val isPreset = ACCENT_PRESETS.any { it.argb == vm.accent }
    var free by remember { mutableStateOf(!isPreset) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ACCENT_PRESETS.forEach { p ->
            val shown = if (p.argb == 0) accentColors(0xFF3F5F8A.toInt(), false).primary else p.argb
            Swatch(Color(shown), selected = !free && vm.accent == p.argb, label = p.name) { free = false; vm.updateAccent(p.argb) }
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (free) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                .clickable { free = true },
            contentAlignment = Alignment.Center
        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
    if (free) {
        val start = remember { argbToHsl(if (vm.accent == 0) 0xFF3F5F8A.toInt() else vm.accent) }
        var hue by remember { mutableFloatStateOf(start[0]) }
        var sat by remember { mutableFloatStateOf(start[1].coerceAtLeast(0.2f)) }
        var light by remember { mutableFloatStateOf(start[2]) }
        val commit = { vm.updateAccent(hslToArgb(hue, sat, light)) }
        val preview = { vm.previewAccent(hslToArgb(hue, sat, light)) }
        Spacer(Modifier.height(8.dp))
        Text("Cor livre", style = MaterialTheme.typography.labelLarge)
        Text("Matiz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(hue, { hue = it; preview() }, onValueChangeFinished = { commit() }, valueRange = 0f..360f)
        Text("Intensidade", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(sat, { sat = it; preview() }, onValueChangeFinished = { commit() }, valueRange = 0f..1f)
        Text("Claridade", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(light, { light = it; preview() }, onValueChangeFinished = { commit() }, valueRange = 0.2f..0.8f)
        Text(
            "O app ajusta a claridade para o texto continuar legível no tema claro e no escuro.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(color)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                .clickable(onClick = onClick)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Mais > Abas e atalhos: aba inicial, ordem da barra de baixo e atalhos do ícone do app. */
@Composable
fun TabsAndShortcutsCard(vm: AppViewModel) {
    NCard(Modifier.fillMaxWidth()) {
        Text("Aba inicial", style = MaterialTheme.typography.titleSmall)
        Text("A que abre quando você entra no app e para onde o botão voltar leva.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.tabOrder.forEach { id ->
                FilterChip(selected = vm.startTab == id, onClick = { vm.updateStartTab(id) }, label = { Text(TAB_LABELS[id]) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Ordem das abas", style = MaterialTheme.typography.titleSmall)
        vm.tabOrder.forEachIndexed { i, id ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}.  ${TAB_LABELS[id]}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { vm.updateTabOrder(moveTab(vm.tabOrder, i, -1)) }, enabled = i > 0) {
                    Icon(Icons.Default.KeyboardArrowUp, "Subir ${TAB_LABELS[id]}")
                }
                IconButton(onClick = { vm.updateTabOrder(moveTab(vm.tabOrder, i, 1)) }, enabled = i < vm.tabOrder.lastIndex) {
                    Icon(Icons.Default.KeyboardArrowDown, "Descer ${TAB_LABELS[id]}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Atalhos do ícone", style = MaterialTheme.typography.titleSmall)
        Text(
            "Segure o ícone do Freya na tela inicial para abrir direto no que você escolher (até $MAX_SHORTCUTS).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SHORTCUTS.forEach { d ->
            val on = d.id in vm.shortcutIds
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(d.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = on,
                    enabled = on || vm.shortcutIds.size < MAX_SHORTCUTS,
                    onCheckedChange = { checked ->
                        vm.setShortcuts(if (checked) vm.shortcutIds + d.id else vm.shortcutIds - d.id)
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Widget de saldo", style = MaterialTheme.typography.titleSmall)
        Text(
            "Ao adicionar o widget na tela inicial você escolhe o que ele mostra, o estilo e o que abre ao tocar. " +
                "Para mudar depois, remova e adicione de novo; em alguns aparelhos, segurar o widget também oferece Configurar.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
