package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.data.Account
import com.japaneixxx.freya.data.isCashAccountId

/**
 * Contas de dinheiro físico: criadas à mão, sem banco. O saldo acompanha os lançamentos feitos nelas
 * (Extrato > +, escolhendo a conta) e entra no saldo total e no patrimônio.
 */
@Composable
fun CashAccountsCard(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val cash = remember(accounts) { accounts.filter { isCashAccountId(it.id) } }
    var creating by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf<Account?>(null) }
    var renaming by remember { mutableStateOf<Account?>(null) }
    var deleting by remember { mutableStateOf<Account?>(null) }

    NCard(Modifier.fillMaxWidth()) {
        Text("Dinheiro físico", style = MaterialTheme.typography.titleMedium)
        Text(
            "Controle o dinheiro vivo: crie uma conta e registre as entradas e saídas dela pelo botão + do Extrato. " +
                "O saldo entra no total e no patrimônio.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        cash.forEach { a ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("💵 ${a.name}", Modifier.weight(1f))
                    AnimatedMoney(a.balance, fontWeight = FontWeight.Medium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { adjusting = a }) { Text("Ajustar saldo") }
                    TextButton(onClick = { renaming = a }) { Text("Renomear") }
                    TextButton(onClick = { deleting = a }) { Text("Excluir") }
                }
            }
        }
        OutlinedButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (cash.isEmpty()) "Criar conta de dinheiro" else "+ Nova conta de dinheiro")
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("Carteira") }
        var value by remember { mutableStateOf("") }
        val amount = if (value.isBlank()) 0.0 else parseAmount(value)
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("Nova conta de dinheiro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.take(40) }, label = { Text("Nome") }, singleLine = true,
                        shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(
                        value, { value = it }, label = { Text("Quanto você tem agora (R$)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && amount != null && amount >= 0,
                    onClick = { vm.addCashAccount(name, amount ?: 0.0); creating = false }
                ) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancelar") } }
        )
    }

    adjusting?.let { acc ->
        var value by remember(acc.id) { mutableStateOf("%.2f".format(acc.balance)) }
        val amount = parseAmount(value)
        AlertDialog(
            onDismissRequest = { adjusting = null },
            title = { Text("Ajustar saldo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Informe quanto você realmente tem. A diferença vira um \"Ajuste de saldo\" no extrato, que não conta como receita nem despesa.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value, { value = it }, label = { Text("Saldo real (R$)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = amount != null && amount >= 0,
                    onClick = { vm.adjustCashBalance(acc.id, amount ?: return@TextButton); adjusting = null }
                ) { Text("Ajustar") }
            },
            dismissButton = { TextButton(onClick = { adjusting = null }) { Text("Cancelar") } }
        )
    }

    renaming?.let { acc -> RenameAccountDialog(vm, acc) { renaming = null } }

    deleting?.let { acc ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Excluir \"${acc.name}\"?") },
            text = { Text("A conta e todos os lançamentos dela serão apagados. Isso não pode ser desfeito (a menos que você tenha um backup).") },
            confirmButton = { TextButton(onClick = { vm.deleteCashAccount(acc.id); deleting = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}
