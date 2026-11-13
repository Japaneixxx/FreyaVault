package com.japaneixxx.freya.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.R

private data class TutorialStep(
    val emoji: String,
    val title: String,
    val lead: String,
    val bullets: List<String>,
    val tip: String? = null,
    val linkLabel: String? = null,
    val url: String? = null
)

private val steps = listOf(
    TutorialStep(
        "🏔️", "Bem-vindo ao Freya",
        "Para ler seus bancos sem servidor nenhum, o app usa o Open Finance pelo Pluggy. É grátis para uso pessoal e leva uns 5 minutos.",
        listOf(
            "Você vai criar uma conta no Meu Pluggy e autorizar seus bancos.",
            "Depois copia 3 informações para cá: Client ID, Client Secret e o Item ID de cada banco.",
            "Tudo fica só neste aparelho, criptografado. O app conversa direto com o Pluggy."
        ),
        tip = "Tenha em mãos o app do seu banco: ele pede uma confirmação durante a autorização."
    ),
    TutorialStep(
        "1️⃣", "Crie sua conta no Meu Pluggy",
        "O Meu Pluggy é a porta de entrada gratuita para pessoas físicas.",
        listOf(
            "Abra o site e crie uma conta (e-mail e senha).",
            "Confirme o e-mail, se ele pedir.",
            "Anote qual e-mail usou: você vai usá-lo de novo mais adiante."
        ),
        linkLabel = "Abrir meu.pluggy.ai", url = "https://meu.pluggy.ai"
    ),
    TutorialStep(
        "2️⃣", "Conecte seus bancos",
        "É aqui que você dá o consentimento do Open Finance.",
        listOf(
            "No Meu Pluggy, adicione cada banco ou corretora que quer acompanhar.",
            "Siga a autorização no app do banco, escolhendo contas e cartões.",
            "Quando cada conexão aparecer como atualizada, pode seguir."
        ),
        tip = "O plano gratuito aceita até 5 conexões. Os dados são atualizados pelo Pluggy cerca de 1 vez por dia."
    ),
    TutorialStep(
        "3️⃣", "Crie uma Application no painel",
        "A Application é o que gera o seu Client ID e o Client Secret.",
        listOf(
            "Entre em dashboard.pluggy.ai com o mesmo e-mail do Meu Pluggy.",
            "Crie uma nova Application (o nome pode ser qualquer um, ex.: Freya).",
            "Copie o Client ID e o Client Secret e guarde num lugar seguro."
        ),
        tip = "O Client Secret costuma aparecer só uma vez. Se perder, gere outro na mesma página.",
        linkLabel = "Abrir dashboard.pluggy.ai", url = "https://dashboard.pluggy.ai"
    ),
    TutorialStep(
        "4️⃣", "Ligue o Meu Pluggy à sua Application",
        "Esse passo cria um Item para cada banco: o Item ID é o que o app usa para ler seus dados.",
        listOf(
            "Na sua Application, abra a tela Demo e toque em Connect Account.",
            "Escolha o conector MeuPluggy (não escolha o banco direto nem o Sandbox).",
            "Entre com a conta do Meu Pluggy e autorize os bancos que quer usar.",
            "Depois, copie o Item ID de cada conexão (ele parece com 207f5bcd-312a-439c-abbe-166b6632c980)."
        ),
        tip = "Se o painel pedir contratação para conectar bancos reais, é sinal de que você escolheu o banco direto em vez do MeuPluggy."
    )
)

@Composable
fun SetupScreen(vm: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = steps.size // o passo final é o formulário
    val ctx = LocalContext.current

    Scaffold { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // cabeçalho com progresso
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Freya", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            stringResource(R.string.developer_name),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (step < last) TextButton(onClick = { step = last }) { Text("Já tenho os dados") }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(steps.size + 1) { i ->
                        val active = i <= step
                        val color by animateColorAsState(
                            targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            animationSpec = tween(360, easing = Motion.EaseOut),
                            label = "onboardingProgress"
                        )
                        Box(
                            Modifier.weight(1f).height(5.dp).background(color, RoundedCornerShape(50))
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (step < last) "Passo ${step + 1} de ${steps.size + 1}" else "Último passo: conectar",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val enter = fadeIn(tween(240)) +
                            slideInHorizontally(tween(320, easing = Motion.EaseOut)) { w ->
                                if (forward) w / 6 else -w / 6
                            }
                        val exit = fadeOut(tween(140)) +
                            slideOutHorizontally(tween(240, easing = Motion.EaseOut)) { w ->
                                if (forward) -w / 10 else w / 10
                            }
                        enter togetherWith exit
                    },
                    label = "onboardingStep"
                ) { current ->
                    if (current < last) StepContent(steps[current]) { url ->
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            .onFailure { vm.setupError = "Não foi possível abrir o navegador." }
                    } else FinalStep(vm)
                }
            }

            // navegação
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) OutlinedButton(onClick = { step -= 1 }) { Text("Voltar") } else Spacer(Modifier.width(1.dp))
                if (step < last) Button(onClick = { step += 1 }) { Text(if (step == 0) "Começar" else "Próximo") }
            }
        }
    }
}

@Composable
private fun StepContent(step: TutorialStep, onOpen: (String) -> Unit) {
    Text(step.emoji, style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(8.dp))
    Text(step.title, style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(step.lead, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
    NCard(Modifier.fillMaxWidth()) {
        step.bullets.forEachIndexed { i, b ->
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(24.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                Spacer(Modifier.width(12.dp))
                Text(b, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (step.url != null && step.linkLabel != null) {
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = { onOpen(step.url) }, modifier = Modifier.fillMaxWidth()) { Text(step.linkLabel) }
    }
    step.tip?.let {
        Spacer(Modifier.height(12.dp))
        NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.secondaryContainer, padding = 14.dp) {
            Text("💡 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun FinalStep(vm: AppViewModel) {
    Text("🔑", style = MaterialTheme.typography.displaySmall)
    Spacer(Modifier.height(8.dp))
    Text("Cole suas credenciais", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        "O app testa a conexão com o Pluggy antes de salvar e avisa exatamente o que está errado.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))
    CredentialsForm(vm, "Testar e conectar")

    Spacer(Modifier.height(20.dp))
    var help by remember { mutableStateOf(false) }
    TextButton(onClick = { help = !help }) { Text(if (help) "Esconder ajuda" else "Deu erro? Veja as causas mais comuns") }
    if (help) {
        NCard(Modifier.fillMaxWidth(), padding = 14.dp) {
            listOf(
                "Erro 401 ou 403" to "Client ID ou Client Secret errados. Copie de novo, sem espaços, e se perdeu o secret gere outro.",
                "Erro 404" to "Item ID errado. Use o Item ID de cada conexão do conector MeuPluggy, não o Client ID.",
                "Só vejo o Sandbox ou pedem contratação" to "Você conectou o banco direto. Volte ao passo 4 e escolha o conector MeuPluggy.",
                "Sem dados depois de conectar" to "Confira se o banco aparece como atualizado no Meu Pluggy. A primeira carga pode levar alguns minutos."
            ).forEach { (t, d) ->
                Text(t, style = MaterialTheme.typography.labelLarge)
                Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp))
            }
        }
    }
}

@Composable
fun CredentialsForm(vm: AppViewModel, buttonLabel: String) {
    var cid by remember { mutableStateOf(vm.settings.clientId) }
    var secret by remember { mutableStateOf(vm.settings.clientSecret) }
    var items by remember { mutableStateOf(vm.settings.itemIds) }
    val clipboard = LocalClipboardManager.current
    fun paste(set: (String) -> Unit) { clipboard.getText()?.text?.trim()?.let(set) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            cid, { cid = it }, label = { Text("Client ID") }, singleLine = true,
            supportingText = { Text("Aparece na sua Application, no dashboard.pluggy.ai") },
            trailingIcon = { TextButton(onClick = { paste { cid = it } }) { Text("Colar") } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            secret, { secret = it }, label = { Text("Client Secret") }, singleLine = true,
            supportingText = { Text("Mostrado uma vez ao criar a Application") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = { TextButton(onClick = { paste { secret = it } }) { Text("Colar") } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            items, { items = it }, label = { Text("Item IDs") },
            supportingText = { Text("Um por linha, até 5 (um para cada banco conectado)") },
            minLines = 2,
            trailingIcon = {
                TextButton(onClick = { paste { items = if (items.isBlank()) it else items.trimEnd() + "\n" + it } }) { Text("Colar") }
            },
            modifier = Modifier.fillMaxWidth()
        )
        vm.setupError?.let {
            NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.errorContainer, padding = 14.dp) {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        if (vm.connecting) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(
            onClick = { vm.testAndSaveCredentials(cid.trim(), secret.trim(), items.trim()) },
            enabled = cid.isNotBlank() && secret.isNotBlank() && items.isNotBlank() && !vm.connecting && !vm.syncing,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (vm.connecting) "Testando conexão…" else buttonLabel) }
    }
}
