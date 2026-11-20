package com.japaneixxx.freya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnTransfersTest {
    private fun tx(id: String, acc: String, desc: String, amount: Double, date: String = "2026-11-10", pending: Boolean = false) =
        Tx(id, acc, date, desc, amount, "", pending)

    @Test fun matchesPixBetweenOwnAccounts() {
        val out = tx("o1", "A", "PIX ENVIADO JOAO SILVA", -100.0)
        val inc = tx("i1", "B", "PIX RECEBIDO JOAO SILVA", 100.0)
        assertEquals(setOf("o1", "i1"), findOwnTransferPairs(listOf(out, inc)))
    }

    @Test fun allowsOneDayOfDifference() {
        val out = tx("o1", "A", "PIX ENVIADO X", -100.0, "2026-11-10")
        val inc = tx("i1", "B", "PIX RECEBIDO X", 100.0, "2026-11-11")
        assertEquals(setOf("o1", "i1"), findOwnTransferPairs(listOf(out, inc)))
        val far = tx("i2", "B", "PIX RECEBIDO X", 100.0, "2026-11-13")
        assertTrue(findOwnTransferPairs(listOf(out, far)).isEmpty())
    }

    @Test fun sameAccountOrDifferentAmountDoesNotMatch() {
        val out = tx("o1", "A", "PIX ENVIADO X", -100.0)
        assertTrue(findOwnTransferPairs(listOf(out, tx("i1", "A", "PIX RECEBIDO X", 100.0))).isEmpty())
        assertTrue(findOwnTransferPairs(listOf(out, tx("i2", "B", "PIX RECEBIDO X", 100.5))).isEmpty())
    }

    @Test fun purchasesAreNeverTransfers() {
        val out = tx("o1", "A", "COMPRA NO DEBITO MERCADO", -100.0)
        val inc = tx("i1", "B", "PIX RECEBIDO AMIGO", 100.0)
        assertTrue(findOwnTransferPairs(listOf(out, inc)).isEmpty())
    }

    @Test fun eachIncomingMatchesOnlyOnce() {
        val a = tx("o1", "A", "PIX ENVIADO X", -100.0)
        val b = tx("o2", "C", "PIX ENVIADO Y", -100.0)
        val inc = tx("i1", "B", "PIX RECEBIDO Z", 100.0)
        assertEquals(2, findOwnTransferPairs(listOf(a, b, inc)).size)
    }

    @Test fun userChosenAndManualAndPendingAreSkipped() {
        val out = tx("o1", "A", "PIX ENVIADO X", -100.0)
        val inc = tx("i1", "B", "PIX RECEBIDO X", 100.0)
        assertTrue(findOwnTransferPairs(listOf(out, inc), skipIds = setOf("o1")).isEmpty())
        assertTrue(findOwnTransferPairs(listOf(out, inc.copy(accountId = CASH_PREFIX + "x"))).isEmpty())
        assertTrue(findOwnTransferPairs(listOf(out, inc.copy(pending = true))).isEmpty())
    }
}
