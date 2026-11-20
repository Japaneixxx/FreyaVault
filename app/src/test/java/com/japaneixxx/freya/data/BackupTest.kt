package com.japaneixxx.freya.data

import com.japaneixxx.freya.ui.KIND_VARIABLE
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupTest {
    private val inTx: suspend (suspend () -> Unit) -> Unit = { it() }

    private fun restore(store: FakeStore, json: String, replace: Boolean,
                        aliases: (Map<String, String>) -> Unit = {}, cats: (List<String>) -> Unit = {}) =
        runBlocking { Backup.import(store.dao, inTx, json, replace, aliases, cats) }

    /** Banco "origem" com um pouco de tudo, inclusive conta de dinheiro. */
    private fun populated(): FakeStore = FakeStore().also { s ->
        runBlocking {
            val d = s.dao
            d.putBudget(Budget("Mercado", 500.0)); d.putGoal(Goal(0, "Viagem", 3000.0, 100.0))
            d.putBill(Bill(0, "Internet", 100.0, 10))
            val luz = d.putBill(Bill(0, "Luz", 150.0, 15, kind = KIND_VARIABLE))
            d.putBillPayment(BillPayment(luz, "2026-10", 140.0))
            d.putIncome(Income(0, "Salário", 5000.0, 5)); d.putOneTimeIncome(OneTimeIncome(0, "Freela", 800.0, "2026-11-20"))
            d.putRule(Rule("uber", "Lazer")); d.putMeta(TxMeta("t1", "nota", true))
            d.putNetItem(NetItem(0, "Carro", "ASSET", 30000.0)); d.putEnvelope(Envelope("Lazer", "2026-10"))
            d.putAccount(Account("cash:1", "manual", "Dinheiro", "Carteira", "BANK", "CASH", 75.5, "BRL"))
            d.putTx(Tx("cash-1", "cash:1", "2026-11-01", "Padaria", -4.5, "Restaurantes", false))
            d.putTx(Tx("manual-1", "manual", "2026-11-02", "Avulso", -10.0, "Outros", false))
            d.putSnapshot(Snapshot("2026-11-01", 10.0, 2.0)); d.putNetSnapshot(NetSnapshot("2026-11-01", 99.0))
        }
    }

    private val json: String by lazy { runBlocking { Backup.export(populated().dao, mapOf("acc" to "Meu banco"), listOf("Pets")) } }

    @Test fun exportCarriesVersionAndEverything() {
        val o = org.json.JSONObject(json)
        assertEquals(Backup.VERSION, o.getInt("version"))
        listOf("budgets", "goals", "bills", "billPayments", "incomes", "oneTimeIncomes", "rules", "metas", "netItems",
            "envelopes", "cashAccounts", "manual", "snapshots", "netSnapshots", "customCategories", "accountAliases")
            .forEach { assertTrue("faltou $it", o.has(it)) }
    }

    @Test fun replaceRebuildsEverythingIncludingCash() {
        val b = FakeStore(); var aliases = emptyMap<String, String>(); var cats = emptyList<String>()
        val n = restore(b, json, true, { aliases = it }, { cats = it })
        assertTrue(n > 10)
        assertEquals(1, b.budgets.size); assertEquals(1, b.goals.size); assertEquals(2, b.bills.size); assertEquals(1, b.payments.size)
        assertEquals(75.5, b.accounts["cash:1"]!!.balance, 0.0)
        assertTrue(b.txs.containsKey("cash-1") && b.txs.containsKey("manual-1"))
        assertEquals(1, b.snaps.size); assertEquals(1, b.netSnaps.size)
        assertEquals(listOf("Pets"), cats); assertEquals("Meu banco", aliases["acc"])
    }

    @Test fun mergingTwiceNeverDuplicates() {
        val c = FakeStore()
        restore(c, json, false); restore(c, json, false)
        assertEquals(1, c.goals.size); assertEquals(2, c.bills.size); assertEquals(1, c.incomes.size)
        assertEquals(1, c.oneTime.size); assertEquals(1, c.netItems.size); assertEquals(1, c.payments.size)
        assertEquals(2, c.txs.size); assertEquals(1, c.accounts.size)
    }

    @Test fun mergeRemapsPaymentsToTheNewBillId() {
        val d = FakeStore()
        runBlocking { d.dao.putBill(Bill(0, "Água", 80.0, 3)) } // ocupa o id 1, que a "Internet" do backup também tinha
        restore(d, json, false)
        val luz = d.bills.values.first { it.name == "Luz" }
        assertEquals(3, d.bills.size)
        assertEquals(luz.id to "2026-10", d.payments.keys.single())
    }

    @Test fun mergeKeepsLocalCashBalanceAndSkipsItsTransactions() {
        val e = FakeStore()
        runBlocking { e.dao.putAccount(Account("cash:1", "manual", "Dinheiro", "Carteira", "BANK", "CASH", 999.0, "BRL")) }
        restore(e, json, false)
        assertEquals(999.0, e.accounts["cash:1"]!!.balance, 0.0)
        assertFalse(e.txs.containsKey("cash-1"))
    }

    @Test fun replaceDropsLocalOnlyItemsOfListsInTheBackup() {
        val f = FakeStore()
        runBlocking { f.dao.putGoal(Goal(0, "Antiga", 1.0)) }
        restore(f, json, true)
        assertEquals(listOf("Viagem"), f.goals.values.map { it.name })
    }

    @Test fun oldBackupDoesNotWipeWhatItDoesNotContain() {
        val g = FakeStore()
        runBlocking { g.dao.putGoal(Goal(0, "Mantida", 1.0)) }
        restore(g, """{"version":1,"budgets":[{"category":"X","limit":1.0}]}""", true)
        assertEquals(1, g.goals.size); assertEquals(1, g.budgets.size)
    }

    @Test fun newerFormatIsRejectedWithAClearMessage() {
        try { restore(FakeStore(), """{"version":99}""", false); fail("deveria rejeitar") }
        catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("mais nova")) }
    }

    @Test fun failureInsideTheTransactionPropagates() {
        // O banco real reverte a transação quando o bloco lança; aqui conferimos que o erro sobe (nada é engolido).
        try { restore(FakeStore(), """{"version":2,"goals":[{"name":"sem target"}]}""", true); fail("deveria falhar") }
        catch (_: org.json.JSONException) {}
    }
}
