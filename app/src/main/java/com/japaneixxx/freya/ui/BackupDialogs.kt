package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Pergunta se o backup deve sair criptografado. [onChoose] recebe a senha, ou null para salvar sem senha. */
@Composable
fun ExportBackupDialog(onChoose: (CharArray?) -> Unit, onDismiss: () -> Unit) {
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val ok = pass.length >= 6 && pass == confirm
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salvar backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "O backup tem seus valores e anotações. Com senha, o arquivo fica criptografado (AES-256). " +
                        "Sem a senha não há como recuperar o conteúdo.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    pass, { pass = it }, label = { Text("Senha (mín. 6 caracteres)") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    confirm, { confirm = it }, label = { Text("Confirmar senha") }, singleLine = true,
                    isError = confirm.isNotEmpty() && confirm != pass,
                    visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onChoose(pass.toCharArray()) }) { Text("Salvar com senha") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onChoose(null) }) { Text("Sem senha") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

/** Escolha entre mesclar ou substituir (e pede a senha quando o arquivo é criptografado). */
@Composable
fun RestoreBackupDialog(vm: AppViewModel, text: String, onDismiss: () -> Unit) {
    val encrypted = remember(text) { vm.isBackupEncrypted(text) }
    var replace by remember { mutableStateOf(false) }
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurar backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (encrypted) {
                    OutlinedTextField(
                        pass, { pass = it }, label = { Text("Senha do backup") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !replace, onClick = { replace = false }, label = { Text("Mesclar") })
                    FilterChip(selected = replace, onClick = { replace = true }, label = { Text("Substituir") })
                }
                Text(
                    if (replace) "Troca, no aparelho, tudo o que o backup contém (orçamentos, metas, contas a pagar, regras...) " +
                        "pelo conteúdo dele. O que estiver só no aparelho e fizer parte dessas listas será apagado."
                    else "Adiciona só o que ainda não existe, sem apagar nem sobrescrever nada e sem duplicar.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("Se algo falhar no meio, nada é alterado.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !encrypted || pass.isNotEmpty(),
                onClick = {
                    vm.restoreBackup(text, replace, if (encrypted) pass.toCharArray() else null)
                    onDismiss()
                }
            ) { Text("Restaurar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
