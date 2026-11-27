package com.japaneixxx.freya

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.data.Settings
import com.japaneixxx.freya.data.WidgetConfig
import com.japaneixxx.freya.ui.AppTheme
import com.japaneixxx.freya.ui.LINK_TARGETS

/** Tela que abre ao adicionar (ou reconfigurar) o widget de saldo. Não mostra nenhum dado financeiro. */
class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val settings = Settings(this)
        setContent {
            AppTheme(settings.themeMode, settings.accentColor, settings.fontScalePct, settings.uiDensity) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ConfigScreen(
                        initial = settings.widgetConfig(id),
                        onSave = { cfg ->
                            settings.setWidgetConfig(id, cfg)
                            BalanceWidget.update(this, id)
                            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigScreen(initial: WidgetConfig, onSave: (WidgetConfig) -> Unit, onCancel: () -> Unit) {
    var show by remember { mutableStateOf(initial.show) }
    var style by remember { mutableStateOf(initial.style) }
    var opens by remember { mutableStateOf(initial.opens) }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Configurar widget", style = MaterialTheme.typography.headlineSmall)
        Text("Escolha o que o widget mostra, como ele fica e o que abre ao tocar.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("O que mostrar", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BOTH" to "Contas e cartões", "BANK" to "Só contas", "CARDS" to "Só faturas").forEach { (k, l) ->
                FilterChip(selected = show == k, onClick = { show = k }, label = { Text(l) })
            }
        }

        Text("Estilo", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("APP" to "Cor do app", "DARK" to "Escuro", "LIGHT" to "Claro", "CLEAR" to "Transparente").forEach { (k, l) ->
                FilterChip(selected = style == k, onClick = { style = k }, label = { Text(l) })
            }
        }

        Text("Ao tocar, abrir", style = MaterialTheme.typography.titleSmall)
        LINK_TARGETS.forEach { t ->
            Row(
                Modifier.fillMaxWidth().selectable(selected = opens == t.id, role = Role.RadioButton) { opens = t.id },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = opens == t.id, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(t.label, Modifier.padding(vertical = 10.dp))
            }
        }
        Text(
            "Com valores ocultos ou bloqueio ativo, o widget nunca mostra números.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = { onSave(WidgetConfig(show, style, opens)) }) { Text("Salvar") }
        }
    }
}
