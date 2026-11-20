package com.japaneixxx.freya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    private fun acc(id: String, type: String, balance: Double, currency: String = "BRL") =
        Account(id, "item", "Banco", "Conta", type, "", balance, currency)

    @Test fun roundingRemovesFloatNoise() {
        assertEquals(0.3, (0.1 + 0.2).r2(), 0.0)
        assertEquals(10.01, 10.005.r2(), 0.0)
    }

    @Test fun totalsIgnoreForeignCurrencyAndIncludeCash() {
        val list = listOf(
            acc("a", "BANK", 100.10), acc(CASH_PREFIX + "1", "BANK", 50.20),
            acc("usd", "BANK", 999.0, "USD"), acc("c", "CREDIT", 30.0), acc("c2", "CREDIT", 10.0, "USD")
        )
        assertEquals(150.30, bankTotal(list), 0.0)
        assertEquals(30.0, cardsTotal(list), 0.0)
        val nw = computeNetWorth(list, emptyList(), emptyList())
        assertEquals(120.30, nw.total, 0.0001)
    }

    @Test fun manualIds() {
        assertTrue(isManualAccountId("manual")); assertTrue(isManualAccountId("cash:abc"))
        assertFalse(isManualAccountId("c8f1-uuid")); assertTrue(isCashAccountId("cash:abc")); assertFalse(isCashAccountId("manual"))
    }
}
