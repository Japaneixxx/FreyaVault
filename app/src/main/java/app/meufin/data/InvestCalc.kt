package app.meufin.data

import kotlin.math.pow

/** Como a rentabilidade do investimento é informada. */
enum class RateMode(val label: String, val unit: String) {
    YEAR("% ao ano", "% a.a."),
    MONTH("% ao mês", "% a.m."),
    CDI("% do CDI", "% do CDI"),
    IPCA("IPCA + taxa", "% a.a. + IPCA")
}

/** Tributação: renda fixa comum usa IR regressivo; LCI/LCA/poupança são isentos para pessoa física. */
enum class TaxMode(val label: String) {
    REGRESSIVE("Tributado (IR regressivo)"),
    EXEMPT("Isento (LCI, LCA, poupança)")
}

data class CalcInput(
    val initial: Double,
    val monthly: Double,
    val months: Int,
    val mode: RateMode,
    val rate: Double,          // valor digitado, na unidade de [mode]
    val cdiYear: Double,       // CDI anual em %, usado só em RateMode.CDI
    val ipcaYear: Double,      // IPCA 12 meses em %, usado só em RateMode.IPCA
    val tax: TaxMode
)

data class CalcPoint(val invested: Double, val gross: Double, val net: Double)

data class CalcResult(
    val points: List<CalcPoint>,   // índice 0 = hoje, índice i = fim do mês i
    val monthlyRate: Double        // taxa efetiva mensal usada (fração, ex.: 0.0095)
) {
    val last: CalcPoint get() = points.last()
    val invested: Double get() = last.invested
    val gross: Double get() = last.gross
    val net: Double get() = last.net
    val grossProfit: Double get() = gross - invested
    val tax: Double get() = gross - net
    val netProfit: Double get() = net - invested
    /** Taxa bruta efetiva equivalente ao ano, para o usuário conferir a conversão. */
    val annualRate: Double get() = (1 + monthlyRate).pow(12) - 1
}

const val MAX_MONTHS = 600

/** Taxa efetiva mensal (fração) a partir do que o usuário digitou. */
fun monthlyRate(input: CalcInput): Double = when (input.mode) {
    RateMode.MONTH -> input.rate / 100.0
    RateMode.YEAR -> (1 + input.rate / 100.0).pow(1.0 / 12) - 1
    RateMode.CDI -> {
        // % do CDI incide sobre a taxa diária (252 dias úteis ao ano, ~21 por mês).
        val daily = (1 + input.cdiYear / 100.0).pow(1.0 / 252) - 1
        (1 + daily * input.rate / 100.0).pow(21.0) - 1
    }
    RateMode.IPCA -> {
        val year = (1 + input.ipcaYear / 100.0) * (1 + input.rate / 100.0) - 1
        (1 + year).pow(1.0 / 12) - 1
    }
}

/** Alíquota regressiva de IR conforme o tempo de aplicação (30 dias por mês). */
fun irRate(months: Int): Double {
    val days = months * 30
    return when {
        days <= 180 -> 0.225
        days <= 360 -> 0.20
        days <= 720 -> 0.175
        else -> 0.15
    }
}

/**
 * Simula mês a mês. O aporte mensal entra no início de cada mês (já rende naquele mês) e o saldo
 * inicial é tratado como aplicado hoje. O valor líquido de cada ponto é o que sobraria se o dinheiro
 * fosse resgatado naquele mês, com o IR calculado lote a lote (cada aporte tem a sua idade).
 * IOF não entra: só incide nos primeiros 30 dias e a simulação é em meses inteiros.
 */
fun simulate(input: CalcInput): CalcResult {
    val n = input.months.coerceIn(1, MAX_MONTHS)
    val points = ArrayList<CalcPoint>(n + 1)
    points += CalcPoint(input.initial, input.initial, input.initial)
    for (t in 1..n) points += pointAt(input, t)
    return CalcResult(points, monthlyRate(input))
}

/** Valores ao fim do mês [t], com os lotes de cada aporte rendendo pelo próprio tempo. */
private fun pointAt(input: CalcInput, t: Int): CalcPoint {
    val growth = 1 + monthlyRate(input)
    val regressive = input.tax == TaxMode.REGRESSIVE
    // lote inicial: t meses de rendimento; aporte do mês k (1..t): t - k + 1 meses
    var gross = input.initial * growth.pow(t)
    var tax = if (regressive) (gross - input.initial).coerceAtLeast(0.0) * irRate(t) else 0.0
    if (input.monthly != 0.0) {
        for (k in 1..t) {
            val age = t - k + 1
            val lot = input.monthly * growth.pow(age)
            gross += lot
            if (regressive) tax += (lot - input.monthly).coerceAtLeast(0.0) * irRate(age)
        }
    }
    return CalcPoint(input.initial + input.monthly * t, gross, gross - tax)
}

/**
 * Meta reversa: aporte mensal necessário para o valor LÍQUIDO chegar a [target] ao fim do prazo.
 * Retorna 0 se só o saldo inicial já basta, ou null se a meta não é alcançável com os dados informados.
 */
fun requiredMonthly(input: CalcInput, target: Double): Double? {
    if (target <= 0.0) return null
    if (netFinal(input.copy(monthly = 0.0)) >= target) return 0.0
    var lo = 0.0
    var hi = target / input.months.coerceAtLeast(1)    // sem juros já bastaria; juros só reduzem o necessário
    if (netFinal(input.copy(monthly = hi)) < target) {
        // juros negativos ou imposto alto: amplia o teto
        var tries = 0
        while (netFinal(input.copy(monthly = hi)) < target && tries < 20) { hi *= 2; tries++ }
        if (netFinal(input.copy(monthly = hi)) < target) return null
    }
    repeat(50) {
        val mid = (lo + hi) / 2
        if (netFinal(input.copy(monthly = mid)) < target) lo = mid else hi = mid
    }
    return hi
}

private fun netFinal(input: CalcInput): Double = pointAt(input, input.months.coerceIn(1, MAX_MONTHS)).net
