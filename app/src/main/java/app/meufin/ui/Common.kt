package app.meufin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/** Botão que abre um menu com a lista de categorias. */
@Composable
fun CategoryPicker(
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }) { Text(selected) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); open = false })
            }
        }
    }
}

fun parseAmount(text: String): Double? = text.trim().replace(",", ".").toDoubleOrNull()
