package com.japaneixxx.freya.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val TRANSFER_KINDS = setOf(
    TxKind.PIX_SENT, TxKind.PIX_RECEIVED, TxKind.TRANSFER_SENT, TxKind.TRANSFER_RECEIVED
)

private fun cents(v: Double): Long = Math.round(abs(v) * 100)

private fun daysBetween(a: String, b: String): Long =
    runCatching { abs(ChronoUnit.DAYS.between(LocalDate.parse(a), LocalDate.parse(b))) }.getOrDefault(99L)

/**
 * Acha os dois lados de transferências entre contas do próprio usuário: uma saída e uma entrada de mesmo valor,
 * em contas diferentes, com até 1 dia de diferença e ambas parecendo Pix/transferência. Cada entrada casa com
 * uma única saída. [skipIds] são lançamentos que o usuário já categorizou à mão e que não devem ser tocados.
 * Devolve os IDs dos dois lados, para não contar a mesma movimentação como despesa e receita.
 */
fun findOwnTransferPairs(txs: List<Tx>, skipIds: Set<String> = emptySet()): Set<String> {
    val candidates = txs.filter {
        !it.pending && it.amount != 0.0 && !isManualAccountId(it.accountId) && it.id !in skipIds
    }
    val incomingByCents = candidates.filter { it.amount > 0 }.groupBy { cents(it.amount) }
    if (incomingByCents.isEmpty()) return emptySet()

    val used = HashSet<String>()
    val result = HashSet<String>()
    for (out in candidates.filter { it.amount < 0 }.sortedBy { it.date }) {
        val options = incomingByCents[cents(out.amount)]
            ?.filter { it.id !in used && it.accountId != out.accountId && daysBetween(it.date, out.date) <= 1 }
            .orEmpty()
        if (options.isEmpty()) continue
        if (sanitize(out.description, out.amount).kind !in TRANSFER_KINDS) continue
        val match = options
            .filter { sanitize(it.description, it.amount).kind in TRANSFER_KINDS }
            .minByOrNull { daysBetween(it.date, out.date) } ?: continue
        used += match.id
        result += match.id
        result += out.id
    }
    return result
}
