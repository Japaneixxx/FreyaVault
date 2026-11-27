package com.japaneixxx.freya.ui

import com.japaneixxx.freya.data.Account
import com.japaneixxx.freya.data.Bill
import com.japaneixxx.freya.data.BillPayment
import com.japaneixxx.freya.data.Budget
import com.japaneixxx.freya.data.CAT_OWN_TRANSFER
import com.japaneixxx.freya.data.Envelope
import com.japaneixxx.freya.data.Income
import com.japaneixxx.freya.data.Rule
import com.japaneixxx.freya.data.Tx
import com.japaneixxx.freya.data.ruleKeyFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class InsightsTest {
    private fun tx(id: String, acc: String, desc: String, amount: Double, date: String = "2026-11-10") =
        Tx(id, acc, date, desc, amount, "", false)

    private fun spend(date: String, amount: Double, category: String = "Lazer") =
        TxItem(tx("s-$date-$amount", "A", "Compra", amount, date), category)

    // ---- buildItems / transferências próprias ----
    @Test fun ownTransferPairIsRecategorized() {
        val items = buildItems(
            listOf(tx("o", "A", "PIX ENVIADO EU MESMO", -100.0), tx("i", "B", "PIX RECEBIDO EU MESMO", 100.0)),
            emptyList(), emptyList()
        )
        assertTrue(items.all { it.category == CAT_OWN_TRANSFER })
        assertTrue(items.all { it.ignored })
    }

    @Test fun userRuleBeatsTransferMatching() {
        val out = tx("o", "A", "PIX ENVIADO EU MESMO", -100.0)
        val inc = tx("i", "B", "PIX RECEBIDO EU MESMO", 100.0)
        val items = buildItems(listOf(out, inc), listOf(Rule(ruleKeyFor(out), "Lazer")), emptyList())
        assertNotEquals(CAT_OWN_TRANSFER, items.first { it.tx.id == "o" }.category)
        assertEquals("Lazer", items.first { it.tx.id == "o" }.category)
    }

    @Test fun metaIgnoredAndNotesAreCarried() {
        val t = tx("x", "A", "Compra qualquer", -5.0)
        val items = buildItems(listOf(t), emptyList(), listOf(com.japaneixxx.freya.data.TxMeta("x", "obs", true)))
        assertEquals("obs", items.single().note); assertTrue(items.single().ignored)
    }

    @Test fun cashAndManualCountAsManualItems() {
        assertTrue(TxItem(tx("a", "cash:1", "x", -1.0), "Outros").manual)
        assertTrue(TxItem(tx("b", "manual", "x", -1.0), "Outros").manual)
        assertFalse(TxItem(tx("c", "acc-9", "x", -1.0), "Outros").manual)
    }

    // ---- envelopes ----
    @Test fun envelopeCarriesLeftoverAndOverspendDoesNotAccumulate() {
        val real = listOf(spend("2026-09-10", -300.0), spend("2026-10-05", -700.0))
        assertEquals(200.0, envelopeCarry("Lazer", 500.0, "2026-09", real, "2026-10"), 0.0001)
        assertEquals(0.0, envelopeCarry("Lazer", 500.0, "2026-09", real, "2026-11"), 0.0001)
        assertEquals(0.0, envelopeCarry("Lazer", 500.0, "lixo", real, "2026-11"), 0.0)
    }

    @Test fun effectiveBudgetsOnlyCarryWhenEnvelopeExists() {
        val real = listOf(spend("2026-09-10", -300.0))
        val list = effectiveBudgets(
            listOf(Budget("Lazer", 500.0), Budget("Mercado", 400.0)), listOf(Envelope("Lazer", "2026-09")), real
        )
        val lazer = list.first { it.category == "Lazer" }
        assertTrue(lazer.envelope); assertTrue(lazer.carry >= 0.0); assertEquals(500.0 + lazer.carry, lazer.available, 0.0001)
        assertFalse(list.first { it.category == "Mercado" }.envelope)
        assertEquals(400.0, list.first { it.category == "Mercado" }.available, 0.0)
    }

    // ---- contas a pagar ----
    @Test fun dueDateClampsToTheEndOfShortMonths() {
        assertEquals(LocalDate.of(2026, 2, 28), billDueDate(Bill(1, "Aluguel", 1.0, 31), YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 11, 10), billDueDate(Bill(1, "Aluguel", 1.0, 10), YearMonth.of(2026, 11)))
    }

    @Test fun installmentNumberAndActivity() {
        val b = Bill(1, "Carro", 900.0, 5, kind = KIND_INSTALLMENT, totalInstallments = 12, firstMonth = "2026-01")
        assertEquals(1, installmentNumber(b, YearMonth.of(2026, 1)))
        assertEquals(3, installmentNumber(b, YearMonth.of(2026, 3)))
        assertTrue(isBillActive(b, YearMonth.of(2026, 12)))
        assertFalse(isBillActive(b, YearMonth.of(2027, 1)))
        assertFalse(isBillActive(b, YearMonth.of(2025, 12)))
        assertEquals(YearMonth.of(2026, 12), installmentEnd(b))
        assertEquals(null, installmentNumber(Bill(2, "Luz", 1.0, 5), YearMonth.of(2026, 3)))
        assertTrue(isBillActive(Bill(2, "Luz", 1.0, 5), YearMonth.of(2030, 1)))
    }

    @Test fun variableBillUsesTheAverageOfTheLastThreePayments() {
        val luz = Bill(7, "Luz", 100.0, 15, kind = KIND_VARIABLE)
        val pays = listOf(
            BillPayment(7, "2026-07", 999.0), BillPayment(7, "2026-08", 100.0),
            BillPayment(7, "2026-09", 200.0), BillPayment(7, "2026-10", 300.0), BillPayment(8, "2026-10", 5.0)
        )
        assertEquals(200.0, billEstimate(luz, pays), 0.0001)
        assertEquals(100.0, billEstimate(luz, emptyList()), 0.0)
        assertEquals(50.0, billEstimate(Bill(9, "Fixa", 50.0, 1), pays), 0.0)
    }

    // ---- receitas ----
    @Test fun businessDayIncomeCountsOnlyWeekdays() {
        // Nov/2026 começa num domingo: 1º dia útil = seg 2, 5º dia útil = sex 6.
        assertEquals(LocalDate.of(2026, 11, 2), incomeDate(Income(1, "Salário", 1.0, 1, true), YearMonth.of(2026, 11)))
        assertEquals(LocalDate.of(2026, 11, 6), incomeDate(Income(1, "Salário", 1.0, 5, true), YearMonth.of(2026, 11)))
    }

    @Test fun fixedDayIncomeOnWeekendMovesBackToFriday() {
        val d = incomeDate(Income(1, "Aluguel", 1.0, 8, false), YearMonth.of(2026, 11)) // 8/11/2026 é domingo
        assertEquals(LocalDate.of(2026, 11, 6), d)
        assertFalse(d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 10), incomeDate(Income(1, "X", 1.0, 10, false), YearMonth.of(2026, 11)))
    }

    // ---- calendário de gastos: entradas x saídas por dia ----
    @Test fun dayFlowsSeparateIncomeAndExpensePerDay() {
        val items = listOf(
            spend("2026-11-03", -80.0), spend("2026-11-03", -20.0),
            TxItem(tx("in", "A", "Salário", 500.0, "2026-11-03"), "Salário"),
            TxItem(tx("in2", "A", "Pix", 40.0, "2026-11-10"), "Outros")
        )
        val f = dayFlows(items)
        assertEquals(500.0, f.getValue(3).income, 0.001)
        assertEquals(100.0, f.getValue(3).expense, 0.001)
        assertEquals(400.0, f.getValue(3).net, 0.001)
        assertEquals(40.0, f.getValue(10).net, 0.001)
        assertFalse(f.containsKey(4))
    }

    // ---- agenda do celular ----
    private fun card(id: String, balance: Double, due: String?) =
        Account(id, "i", "Nubank", "Roxinho", "CREDIT", "CREDIT_CARD", balance, "BRL", dueDate = due)

    @Test fun calendarWishesListsOnlyUpcomingCardsWithBalance() {
        val today = LocalDate.of(2026, 11, 26)
        val w = calendarWishes(
            listOf(card("a", 300.0, "2026-12-05"), card("b", 0.0, "2026-12-06"), card("c", 90.0, "2026-11-01"), card("d", 50.0, "2027-03-01")),
            emptyList(), emptyList(), emptyMap(), false, null, today
        )
        assertEquals(listOf("card:a:2026-12-05"), w.map { it.key })
        assertFalse(w.first().description.contains("R$"))
    }

    @Test fun calendarWishesWritesAmountOnlyWhenAsked() {
        val today = LocalDate.of(2026, 11, 26)
        val w = calendarWishes(listOf(card("a", 300.0, "2026-12-05")), emptyList(), emptyList(), emptyMap(), false, { "R$ ${it.toInt()}" }, today)
        assertTrue(w.first().description.contains("R$ 300"))
    }

    @Test fun calendarWishesIncludesBillsOnlyWhenEnabled() {
        val today = LocalDate.of(2026, 11, 26)
        val bill = Bill(id = 7, name = "Internet", amount = 100.0, dueDay = 28)
        val off = calendarWishes(emptyList(), listOf(bill), emptyList(), emptyMap(), false, null, today)
        val on = calendarWishes(emptyList(), listOf(bill), emptyList(), emptyMap(), true, null, today)
        assertTrue(off.isEmpty())
        assertEquals("bill:7:2026-11-28", on.first().key)
    }
}
