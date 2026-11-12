package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.data.Account

/** Diálogo para dar um nome próprio a uma conta ou cartão. O nome do banco não é alterado. */
@Composable
fun RenameAccountDialog(vm: AppViewModel, account: Account, onDismiss: () -> Unit) {
    val original = vm.originalAccountName(account.id).ifBlank { account.name }
    val hasAlias = vm.hasAccountAlias(account.id)
    var text by remember(account.id) { mutableStateOf(account.name) }
    val isCard = account.type == "CREDIT"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCard) "Renomear cartão" else "Renomear conta") },
        text = {
            Column {
                Text(
                    "${account.institution} · nome no banco: $original",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(40) },
                    label = { Text("Novo nome") }, singleLine = true, shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Muda só dentro do Freya e continua valendo depois de cada sincronização.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    val name = text.trim()
                    vm.renameAccount(account.id, if (name == original) null else name)
                    onDismiss()
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (hasAlias) TextButton(onClick = { vm.renameAccount(account.id, null); onDismiss() }) { Text("Restaurar original") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
