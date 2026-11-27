package com.japaneixxx.freya.ui

import com.japaneixxx.freya.data.CAT_CARD_PAYMENT
import com.japaneixxx.freya.data.Tx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyclesTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun item(id: String, date: String, amount: Double, cat: String = "Lazer", acc: String = "C1") =
        TxItem(Tx(id, acc, date, "Compra $id", amount, "", false), cat)

    @Test fun cycleBeforeClosingDayStartsInPreviousMonth() {
        val c = cycleContaining(d("2026-11-26"), 5)
        assertEquals(d("2026-11-05"), c.start)
        assertEquals(d("2026-12-04"), c.end)
        assertEquals(d("2026-12-05"), c.closing)
    }

    @Test fun purchaseOnClosingDayGoesToNextCycle() {
        val c = cycleContaining(d("2026-11-05"), 5)
        assertEquals(d("2026-11-05"), c.start)
        val before = cycleContaining(d("2026-11-04"), 5)
        assertEquals(d("2026-11-04"), before.end)
    }

    @Test fun closingDay31FallsOnLastDayOfShortMonths() {
        val c = cycleContaining(d("2026-02-10"), 31)
        assertEquals(d("2026-01-31"), c.start)
        assertEquals(d("2026-02-27"), c.end)
        assertEquals(d("2026-02-28"), c.closing)
    }

    @Test fun recentCyclesAreContiguousAndNewestFirst() {
        val list = recentCycles(d("2026-11-26"), 5, 4)
        assertEquals(4, list.size)
        for (i in 0 until list.lastIndex) assertEquals(list[i].start.minusDays(1), list[i + 1].end)
        assertTrue(!list[0].start.isAfter(d("2026-11-26")) && !list[0].end.isBefore(d("2026-11-26")))
    }

    @Test fun estimatedDueIsAfterClosing() {
        val c = cycleContaining(d("2026-11-26"), 5) // fecha 05/12
        assertEquals(d("2026-12-12"), estimatedDue(c, 12))
        assertEquals(d("2027-01-01"), estimatedDue(c, 1))
    }

    @Test fun summarySumsPurchasesMinusRefundsAndIgnoresCardPayments() {
        val cycle = cycleContaining(d("2026-11-26"), 5)
        val items = listOf(
            item("1", "2026-11-10", -100.0),
            item("2", "2026-11-20", -50.0, "Mercado"),
            item("3", "2026-11-22", 30.0),                       // estorno
            item("4", "2026-11-25", 500.0, CAT_CARD_PAYMENT),   // pagamento da fatura anterior
            item("5", "2026-11-04", -999.0),                     // ciclo anterior
            item("6", "2026-11-10", -10.0, acc = "OUTRO")
        ).filter { it.tx.accountId == "C1" }
        val s = summarizeCycle(items, cycle)
        assertEquals(120.0, s.total, 0.001)
        assertEquals("Lazer", s.byCategory.first().first)
        assertEquals(4, s.items.size)
    }
}
