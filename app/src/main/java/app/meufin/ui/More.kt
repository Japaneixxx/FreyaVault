package app.meufin.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import app.meufin.Alerts
import app.meufin.Biometric
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(vm: AppViewModel) {
    val accounts by vm.accounts.collectAsState()
    val rules by vm.rules.collectAsState()
    val groups = remember(accounts) { accounts.groupBy { it.institution } }
    var confirmWipe by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val csvExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.buildCsv().toByteArray(Charsets.UTF_8)) }
        }.onFailure { vm.message = "Não foi possível exportar" }
    }
    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = vm.backupJson()
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                vm.message = "Backup salvo"
            }.onFailure { vm.message = "Não foi possível salvar o backup" }
        }
    }
    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            if (text != null) vm.restoreBackup(text) else vm.message = "Não foi possível ler o arquivo"
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setAlerts(granted)
        if (!granted) vm.message = "Permissão de notificações negada"
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Mais", style = MaterialTheme.typography.headlineMedium)

        SectionTitle("Contas")
        if (accounts.isEmpty()) Text("Nenhuma conta ainda. Toque em sincronizar.", style = MaterialTheme.typography.bodyMedium)
        groups.forEach { (inst, list) ->
            NCard(Modifier.fillMaxWidth()) {
                Text(inst, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                list.forEach { a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(a.name)
                            Text(
                                if (a.type == "CREDIT") "Cartão de crédito" else "Conta",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(money(a.balance), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        SectionTitle("Segurança e avisos")
        NCard(Modifier.fillMaxWidth()) {
            SwitchRow("Bloquear com biometria", "Pede digital, rosto ou PIN ao abrir.", vm.lockEnabled) { on ->
                if (!on) vm.setLock(false)
                else if (!Biometric.available(ctx)) vm.message = "Configure biometria ou bloqueio de tela no Android primeiro"
                else {
                    val act = ctx as? FragmentActivity
                    if (act == null) vm.setLock(true)
                    else Biometric.prompt(act, onSuccess = { vm.setLock(true) })
                }
            }
            Spacer(Modifier.height(10.dp))
            SwitchRow("Alertas", "Avisa quando um orçamento chega a 80%/100%, e antes do vencimento de contas e faturas.", vm.alertsEnabled) { on ->
                if (!on) vm.setAlerts(false)
                else if (Build.VERSION.SDK_INT >= 33 && !Alerts.canNotify(ctx)) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.setAlerts(true)
            }
            Spacer(Modifier.height(10.dp))
            SwitchRow("Ocultar valores", "Esconde saldos e valores em todo o app.", Privacy.hidden) { vm.toggleHide() }
        }

        SectionTitle("Aparência")
        NCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sistema", "Claro", "Escuro").forEachIndexed { i, l ->
                    FilterChip(selected = vm.themeMode == i, onClick = { vm.setTheme(i) }, label = { Text(l) })
                }
            }
        }

        SectionTitle("Dados")
        NCard(Modifier.fillMaxWidth()) {
            Text("Última sincronização: ${ago(vm.lastSync)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "O Meu Pluggy atualiza os bancos a cada ~24h; o app apenas busca o que já está lá.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { vm.sync() }, enabled = !vm.syncing, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vm.syncing) "Sincronizando…" else "Sincronizar agora")
                }
                OutlinedButton(onClick = {
                    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, vm.monthSummary()) }
                    ctx.startActivity(Intent.createChooser(i, "Compartilhar resumo do mês"))
                }, modifier = Modifier.fillMaxWidth()) { Text("Compartilhar resumo do mês") }
                OutlinedButton(onClick = { csvExporter.launch("meu-financeiro.csv") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Exportar transações (CSV)")
                }
                OutlinedButton(onClick = { showRules = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Minhas regras de categoria (${rules.size})")
                }
            }
        }

        SectionTitle("Backup")
        NCard(Modifier.fillMaxWidth()) {
            Text(
                "Salva orçamentos, metas, contas a pagar, regras, notas e lançamentos manuais (o que não vem do banco).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { backupExporter.launch("meu-financeiro-backup.json") }) { Text("Salvar backup") }
                OutlinedButton(onClick = { backupImporter.launch(arrayOf("application/json", "*/*")) }) { Text("Restaurar") }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = { confirmWipe = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Apagar dados locais")
            }
        }

        SectionTitle("Credenciais Pluggy")
        NCard(Modifier.fillMaxWidth()) { CredentialsForm(vm, "Salvar e sincronizar") }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Apagar dados locais?") },
            text = { Text("Remove contas, transações, categorias, orçamentos, metas e contas a pagar do aparelho. Faça um backup antes. Suas credenciais continuam salvas.") },
            confirmButton = { TextButton(onClick = { vm.wipeLocalData(); confirmWipe = false }) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancelar") } }
        )
    }
    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("Regras de categoria") },
            confirmButton = { TextButton(onClick = { showRules = false }) { Text("Fechar") } },
            text = {
                if (rules.isEmpty()) {
                    Text("Nenhuma regra ainda. Ao trocar a categoria de uma transação, o app aprende e aplica às parecidas.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(rules.sortedBy { it.ruleKey }, key = { it.ruleKey }) { r ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.ruleKey, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    Text("→ ${r.category}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { vm.deleteRule(r.ruleKey) }) { Text("Remover") }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SetupScreen(vm: AppViewModel) {
    Scaffold { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("Meu Financeiro", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Seus dados ficam só neste aparelho. As credenciais são guardadas criptografadas e o app fala direto com o Pluggy.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "1. Conecte seus bancos em meu.pluggy.ai\n" +
                        "2. No dashboard.pluggy.ai, crie uma Application, abra a tela Demo e conecte o conector MeuPluggy\n" +
                        "3. Copie o Client ID, o Client Secret e o Item ID de cada banco (até 5)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(20.dp))
            CredentialsForm(vm, "Conectar")
            if (vm.syncing) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            vm.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun CredentialsForm(vm: AppViewModel, buttonLabel: String) {
    var cid by remember { mutableStateOf(vm.settings.clientId) }
    var secret by remember { mutableStateOf(vm.settings.clientSecret) }
    var items by remember { mutableStateOf(vm.settings.itemIds) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(cid, { cid = it }, label = { Text("Client ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            secret, { secret = it }, label = { Text("Client Secret") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            items, { items = it }, label = { Text("Item IDs (vírgula ou linha)") },
            minLines = 2, modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { vm.saveCredentials(cid.trim(), secret.trim(), items.trim()) },
            enabled = cid.isNotBlank() && secret.isNotBlank() && items.isNotBlank() && !vm.syncing,
            modifier = Modifier.fillMaxWidth()
        ) { Text(buttonLabel) }
    }
}
