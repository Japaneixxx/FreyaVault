package com.japaneixxx.freya.data

/** Identificadores das contas criadas à mão. */
const val MANUAL_ACCOUNT_ID = "manual"
const val CASH_PREFIX = "cash:"
const val MANUAL_ITEM_ID = "manual"

/** Lançamento sem conta ("manual") ou lançamento numa conta de dinheiro físico. */
fun isManualAccountId(id: String): Boolean = id == MANUAL_ACCOUNT_ID || id.startsWith(CASH_PREFIX)

fun isCashAccountId(id: String): Boolean = id.startsWith(CASH_PREFIX)

/** Arredonda para centavos: evita que somas de Double deixem restos como 0.30000000000000004. */
fun Double.r2(): Double = Math.round(this * 100.0) / 100.0

/** Só moeda real entra nas somas: um cartão em dólar não pode virar reais. */
private fun Account.isBrl() = currency.isBlank() || currency.equals("BRL", ignoreCase = true)

fun bankTotal(accounts: List<Account>): Double =
    accounts.filter { it.type == "BANK" && it.isBrl() }.sumOf { it.balance }.r2()

fun cardsTotal(accounts: List<Account>): Double =
    accounts.filter { it.type == "CREDIT" && it.isBrl() }.sumOf { it.balance }.r2()
