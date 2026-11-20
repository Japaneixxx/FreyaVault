package com.japaneixxx.freya.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** O DatePicker do Material trabalha em UTC: usar o fuso do aparelho faria a data andar um dia para trás no Brasil. */
private val UTC = ZoneId.of("UTC")

/**
 * Campo de data que abre o calendário ao toque. [date] e o retorno de [onDateChange] são ISO (yyyy-MM-dd).
 *
 * Um OutlinedTextField somente leitura consome o toque (o `clickable` do pai nunca é chamado), por isso o clique
 * fica numa camada transparente por cima do campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    date: String,
    onDateChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    shape: Shape = OutlinedTextFieldDefaults.shape
) {
    var show by remember { mutableStateOf(false) }
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()

    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = parsed?.let { dateBrLabel(it.toString()) } ?: date,
            onValueChange = {},
            label = { Text(label) },
            singleLine = true,
            readOnly = true,
            isError = parsed == null,
            shape = shape,
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }

    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = parsed?.atStartOfDay(UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onDateChange(Instant.ofEpochMilli(it).atZone(UTC).toLocalDate().toString())
                    }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = state)
        }
    }
}
