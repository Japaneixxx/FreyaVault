package com.japaneixxx.freya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorizerTest {
    private fun tx(desc: String, amount: Double = -50.0, category: String = "", account: String = "acc1") =
        Tx(id = "t-$desc-$amount", accountId = account, date = "2026-11-10", description = desc,
            amount = amount, pluggyCategory = category, pending = false)

    private fun cat(t: Tx) = categorize(t, emptyMap())

    @Test fun uberIsTransport() = assertEquals("Transporte", cat(tx("UBER *TRIP HELP.UBER.COM")))

    @Test fun keywordMustStartAWord() {
        // "uber" dentro de "tuberculose" e "posto" dentro de "composto" não podem casar
        assertFalse(cat(tx("Exame tuberculose")) == "Transporte")
        assertFalse(cat(tx("Material composto")) == "Transporte")
        assertTrue(hasWordStart("uber trip", "uber"))
        assertFalse(hasWordStart("tuberculose", "uber"))
    }

    @Test fun mercadoLivreIsShoppingNotGroceries() {
        assertEquals("Compras", cat(tx("MERCADO LIVRE*PEDIDO")))
        assertEquals("Mercado", cat(tx("Mercado Central da Vila")))
    }

    @Test fun digitsKeywordsWork() = assertEquals("Transporte", cat(tx("99APP*VIAGEM")))

    @Test fun unknownEnglishPluggyCategoryDoesNotLeak() {
        assertEquals("Outros", cat(tx("Loja qualquer", category = "Pet supplies")))
        assertEquals("Receitas", cat(tx("Entrada qualquer", amount = 80.0, category = "Pet supplies")))
    }

    @Test fun knownPluggyCategoryIsTranslated() = assertEquals("Saúde", cat(tx("Qualquer", category = "Healthcare")))

    @Test fun userRuleWins() {
        val t = tx("UBER *TRIP")
        assertEquals("Lazer", categorize(t, mapOf(ruleKeyFor(t) to "Lazer")))
    }

    @Test fun manualAndCashKeepTheirOwnCategory() {
        assertEquals("Lazer", cat(tx("Qualquer", category = "Lazer", account = MANUAL_ACCOUNT_ID)))
        assertEquals("Lazer", cat(tx("Qualquer", category = "Lazer", account = CASH_PREFIX + "abc")))
    }
}
