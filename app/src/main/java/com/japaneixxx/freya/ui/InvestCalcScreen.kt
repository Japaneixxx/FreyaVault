package com.japaneixxx.freya.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.japaneixxx.freya.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.util.Locale
import kotlin.math.floor

private data class Scenario(val label: String, val net: Double, val invested: Double)

private val EXEMPT_NAME = Regex("LCI|LCA|CRI|CRA|POUPAN|ISENT", RegexOption.IGNORE_CASE)

/** Número com ponto decimal, que o [parseAmount] entende em qualquer idioma do aparelho. */
private fun fmtNum(v: Double): String = String.format(Locale.US, "%.2f", v)

/** Simulador de investimento: juros compostos com aportes, CDI/IPCA, IR regressivo, meta reversa e comparação. */
@Composable
fun InvestCalcScreen(vm: AppViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val settings = vm.settings
    val investments by vm.investments.collectAsState()
    val txs by vm.txs.collectAsState()
    val scope = rememberCoroutineScope()

    var initialText by rememberSaveable { mutableStateOf("") }
    var monthlyText by rememberSaveable { mutableStateOf("") }
    var monthsText by rememberSaveable { mutableStateOf("24") }
    var modeName by rememberSaveable { mutableStateOf(RateMode.CDI.name) }
    var rateText by rememberSaveable { mutableStateOf("100") }
    var taxName by rememberSaveable { mutableStateOf(TaxMode.REGRESSIVE.name) }
    var cdiText by rememberSaveable { mutableStateOf(settings.calcCdi) }
    var ipcaText by rememberSaveable { mutableStateOf(settings.calcIpca) }
    var ratesInfo by rememberSaveable {
        mutableStateOf(if (settings.calcRatesDate.isNotBlank()) "Banco Central, ${settings.calcRatesDate}" else "")
    }
    var targetText by rememberSaveable { mutableStateOf("") }
    var fillNote by rememberSaveable { mutableStateOf<String?>(null) }
    var fetching by remember { mutableStateOf(false) }
    var ratesError by remember { mutableStateOf<String?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val scenarios = remember { mutableStateListOf<Scenario>() }

    /** Busca CDI e IPCA no Banco Central. No modo [silent] (automático) não mexe no que o usuário digitou nem mostra erro. */
    fun fetchRates(silent: Boolean) {
        if (fetching) return
        scope.launch {
            fetching = true
            if (!silent) ratesError = null
            val beforeCdi = cdiText
            val beforeIpca = ipcaText
            try {
                val r = withContext(Dispatchers.IO) { MarketRatesApi.fetch() }
                settings.calcCdi = fmtNum(r.cdi)
                settings.calcIpca = fmtNum(r.ipca)
                settings.calcRatesDate = r.cdiDate
                settings.calcRatesFetchedAt = System.currentTimeMillis()
                if (!silent || cdiText == beforeCdi) cdiText = fmtNum(r.cdi)
                if (!silent || ipcaText == beforeIpca) ipcaText = fmtNum(r.ipca)
                ratesInfo = "Banco Central · CDI de ${r.cdiDate} · IPCA de ${r.ipcaDate}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                if (!silent) ratesError = "Sem conexão com a internet. Digite as taxas à mão."
            } catch (e: Exception) {
                if (!silent) ratesError = e.message ?: "Não foi possível buscar as taxas."
            } finally {
                fetching = false
            }
        }
    }

    // Atualização automática uma vez por dia; se falhar, ficam as taxas salvas ou digitadas.
    LaunchedEffect(Unit) {
        if (System.currentTimeMillis() - settings.calcRatesFetchedAt > 24L * 60 * 60 * 1000) fetchRates(silent = true)
    }

    fun fillFrom(inv: Investment) {
        initialText = fmtNum(inv.balance)
        val r = inv.annualRate?.takeIf { it > 0 } ?: inv.lastTwelveMonthsRate?.takeIf { it > 0 }
        if (r != null) { modeName = RateMode.YEAR.name; rateText = fmtNum(r) }
        taxName = if (EXEMPT_NAME.containsMatchIn(inv.name)) TaxMode.EXEMPT.name else TaxMode.REGRESSIVE.name
        fillNote = "Preenchido com ${inv.name}. O saldo é tratado como aplicado hoje (o IR real depende da data de cada aplicação) " +
            "e a taxa vem do banco: confira." +
            if (inv.type != "FIXED_INCOME") " Para fundos, ações e outros, é só uma aproximação." else ""
    }

    // Sobra média dos últimos 3 meses fechados (receitas − despesas), arredondada para baixo em dezenas.
    val avgLeft: Double? = remember(txs) {
        val real = txs.filter { !it.ignored }
        val vals = (1..3).map { real.inMonth(monthKey(it)) }.filter { it.isNotEmpty() }.map { it.incomeSum() - it.expenseSum() }
        if (vals.isEmpty()) null else (floor(vals.average() / 10) * 10).takeIf { it > 0 }
    }

    val mode = RateMode.valueOf(modeName)
    val tax = TaxMode.valueOf(taxName)
    val initial = parseAmount(initialText) ?: 0.0
    val monthly = parseAmount(monthlyText) ?: 0.0
    val months = monthsText.toIntOrNull()
    val rate = parseAmount(rateText)
    val cdi = parseAmount(cdiText)
    val ipca = parseAmount(ipcaText)

    val missing: String? = when {
        months == null || months < 1 -> "Informe o prazo em meses."
        rate == null -> "Informe a rentabilidade."
        mode == RateMode.CDI && cdi == null -> "Informe o CDI atual ou toque em Buscar do Banco Central."
        mode == RateMode.IPCA && ipca == null -> "Informe o IPCA ou toque em Buscar do Banco Central."
        initial <= 0.0 && monthly <= 0.0 -> "Informe o valor inicial ou o aporte mensal."
        else -> null
    }
    val input: CalcInput? = if (missing == null) {
        CalcInput(initial.coerceAtLeast(0.0), monthly.coerceAtLeast(0.0), months!!.coerceAtMost(MAX_MONTHS), mode, rate!!, cdi ?: 0.0, ipca ?: 0.0, tax)
    } else null
    val result = remember(input) { input?.let { simulate(it) } }

    val target = parseAmount(targetText)
    val needed = remember(input, target) { if (input != null && target != null && target > 0) requiredMonthly(input, target) else null }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Voltar") }
                Spacer(Modifier.width(4.dp))
                Text("Simulador", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // ---------- dados ----------
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Investimento")
                Spacer(Modifier.height(10.dp))
                if (investments.isNotEmpty()) {
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Preencher com um investimento meu")
                    }
                    fillNote?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                NumField(initialText, { initialText = it }, "Valor inicial (R$)")
                Spacer(Modifier.height(8.dp))
                NumField(monthlyText, { monthlyText = it }, "Aporte mensal (R$)")
                avgLeft?.let { left ->
                    Spacer(Modifier.height(6.dp))
                    AssistChip(
                        onClick = { monthlyText = fmtNum(left) },
                        label = { Text("Usar minha sobra média: ${money(left)}/mês") }
                    )
                }
                Spacer(Modifier.height(8.dp))
                NumField(monthsText, { monthsText = it.filter(Char::isDigit).take(3) }, "Prazo (meses)", integer = true)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12 to "1 ano", 24 to "2 anos", 60 to "5 anos", 120 to "10 anos").forEach { (m, label) ->
                        FilterChip(selected = months == m, onClick = { monthsText = m.toString() }, label = { Text(label) })
                    }
                }
            }
        }

        // ---------- rentabilidade ----------
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Rentabilidade")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RateMode.values().forEach { m ->
                        FilterChip(selected = mode == m, onClick = { modeName = m.name }, label = { Text(m.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                NumField(rateText, { rateText = it }, "Taxa (${mode.unit})")
                result?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Equivale a ${pct(it.annualRate * 100)} ao ano (bruto), ${pct(it.monthlyRate * 100)} ao mês.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (mode == RateMode.CDI || mode == RateMode.IPCA) {
                    Spacer(Modifier.height(10.dp))
                    if (mode == RateMode.CDI) {
                        NumField(cdiText, { cdiText = it; settings.calcCdi = it; ratesInfo = "Valor digitado" }, "CDI atual (% ao ano)")
                    } else {
                        NumField(ipcaText, { ipcaText = it; settings.calcIpca = it; ratesInfo = "Valor digitado" }, "IPCA 12 meses (%)")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { fetchRates(silent = false) }, enabled = !fetching) {
                            Text(if (fetching) "Buscando…" else "Buscar do Banco Central")
                        }
                        if (fetching) {
                            Spacer(Modifier.width(10.dp))
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                    ratesError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (ratesInfo.isNotBlank() && ratesError == null) {
                        Spacer(Modifier.height(4.dp))
                        Text(ratesInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaxMode.values().forEach { t ->
                        FilterChip(selected = tax == t, onClick = { taxName = t.name }, label = { Text(t.label) })
                    }
                }
            }
        }

        // ---------- resultado ----------
        item {
            if (result == null || input == null) {
                NCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Resultado")
                    Spacer(Modifier.height(6.dp))
                    Text(missing ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                NCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.primaryContainer) {
                    val c = MaterialTheme.colorScheme.onPrimaryContainer
                    Text(
                        if (tax == TaxMode.REGRESSIVE) "Você terá, já descontado o IR" else "Você terá",
                        style = MaterialTheme.typography.labelLarge, color = c.copy(alpha = 0.8f)
                    )
                    AnimatedMoney(
                        value = result.net,
                        style = MaterialTheme.typography.displaySmall,
                        color = c,
                        durationMillis = 1200
                    )
                    Text(
                        "em ${input.months} ${if (input.months == 1) "mês" else "meses"}",
                        style = MaterialTheme.typography.bodySmall, color = c.copy(alpha = 0.8f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                NCard(Modifier.fillMaxWidth()) {
                    SectionTitle("Detalhes")
                    Spacer(Modifier.height(8.dp))
                    ResultRow("Total aportado", result.invested, null)
                    ResultRow("Rendimento bruto", result.grossProfit, IncomeColor, plus = true)
                    if (tax == TaxMode.REGRESSIVE) ResultRow("Imposto de renda", result.tax, ExpenseColor, minus = true)
                    ResultRow("Rendimento líquido", result.netProfit, IncomeColor, plus = true, bold = true)
                    Spacer(Modifier.height(12.dp))
                    GrowthChart(result.points.map { it.invested }, result.points.map { it.net })
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Legend(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), "Aportado")
                        Legend(MaterialTheme.colorScheme.primary, "Valor líquido")
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (tax == TaxMode.REGRESSIVE)
                            "IR regressivo: 22,5% até 6 meses, 20% até 1 ano, 17,5% até 2 anos e 15% depois; cada aporte conta o próprio prazo. " +
                                "Aportes entram no início de cada mês. IOF não entra (só nos primeiros 30 dias). É uma estimativa, não uma promessa de rentabilidade."
                        else "Aportes entram no início de cada mês. É uma estimativa, não uma promessa de rentabilidade.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val rateLabel = when (mode) {
                                RateMode.CDI -> "$rateText% do CDI"
                                RateMode.YEAR -> "$rateText% a.a."
                                RateMode.MONTH -> "$rateText% a.m."
                                RateMode.IPCA -> "IPCA + $rateText%"
                            }
                            scenarios += Scenario(
                                "$rateLabel · ${if (tax == TaxMode.REGRESSIVE) "tributado" else "isento"} · ${input.months}m",
                                result.net, result.invested
                            )
                        },
                        enabled = scenarios.size < 3,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (scenarios.size < 3) "Adicionar à comparação" else "Comparação cheia (máx. 3)") }
                }
            }
        }

        // ---------- comparação ----------
        if (scenarios.isNotEmpty()) item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Comparação")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mude os dados acima e adicione outra opção para comparar (ex.: 100% × 110% do CDI, tributado × isento).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val best = scenarios.maxOf { it.net }.coerceAtLeast(1.0)
                scenarios.forEachIndexed { i, s ->
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("aportado ${money(s.invested)}", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AnimatedMoney(s.net, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { scenarios.removeAt(i) }) { Text("Remover") }
                    }
                    NProgress((s.net / best).toFloat(), if (s.net >= best) IncomeColor else MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ---------- meta reversa ----------
        item {
            NCard(Modifier.fillMaxWidth()) {
                SectionTitle("Meta reversa")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Quanto aportar por mês para chegar a um valor, com a rentabilidade, o prazo e o valor inicial acima.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                NumField(targetText, { targetText = it }, "Quero juntar (R$, líquido)")
                if (input != null && target != null && target > 0) {
                    Spacer(Modifier.height(10.dp))
                    when {
                        needed == null -> Text(
                            "Com esses dados não dá para chegar nessa meta.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error
                        )
                        needed == 0.0 -> Text(
                            "O valor inicial sozinho já chega lá no prazo.",
                            style = MaterialTheme.typography.bodyMedium, color = IncomeColor
                        )
                        else -> {
                            Text("Aporte necessário por mês", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AnimatedMoney(needed, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = { monthlyText = fmtNum(needed) }) { Text("Usar como aporte") }
                        }
                    }
                } else if (missing != null && target != null && target > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(missing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Escolha um investimento") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    investments.forEach { inv ->
                        TextButton(onClick = { fillFrom(inv); showPicker = false }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(inv.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${inv.institution} · ${money(inv.balance)}",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, label: String, integer: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ResultRow(label: String, value: Double, color: Color?, plus: Boolean = false, minus: Boolean = false, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        val tint = color ?: MaterialTheme.colorScheme.onSurface
        if (plus || minus) {
            Text(
                if (plus) "+ " else "− ",
                color = tint,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        AnimatedMoney(
            value = value,
            color = tint,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
