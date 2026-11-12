package app.meufin.data

import java.time.LocalDate

/** Patrimônio líquido = contas + investimentos + bens - dívidas - faturas de cartão. */
data class NetWorth(
    val bank: Double,
    val investments: Double,
    val assets: Double,
    val debts: Double,
    val cards: Double
) {
    val total: Double get() = bank + investments + assets - debts - cards
}

fun computeNetWorth(accounts: List<Account>, investments: List<Investment>, items: List<NetItem>): NetWorth =
    NetWorth(
        bank = accounts.filter { it.type == "BANK" }.sumOf { it.balance },
        investments = investments.sumOf { it.balance },
        assets = items.filter { it.kind == "ASSET" }.sumOf { it.amount },
        debts = items.filter { it.kind == "DEBT" }.sumOf { it.amount },
        cards = accounts.filter { it.type == "CREDIT" }.sumOf { it.balance }
    )

/** Grava a foto do dia (a última do dia vence). */
suspend fun FinDao.recordNetSnapshot() {
    val nw = computeNetWorth(accountsNow(), investmentsNow(), netItemsNow())
    putNetSnapshot(NetSnapshot(LocalDate.now().toString(), nw.total))
}

/** Nomes amigáveis dos tipos de investimento do Pluggy. */
fun investmentTypeLabel(type: String): String = when (type) {
    "FIXED_INCOME" -> "Renda fixa"
    "EQUITY" -> "Ações"
    "MUTUAL_FUND" -> "Fundos"
    "ETF" -> "ETFs"
    "COE" -> "COE"
    "SECURITY" -> "Previdência e títulos"
    else -> "Outros"
}
