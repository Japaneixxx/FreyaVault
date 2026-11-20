package com.japaneixxx.freya.data

import java.lang.reflect.Proxy

/** DAO em memória (via Proxy) com só o que o Backup usa; evita precisar de Room/Android nos testes. */
class FakeStore {
    val budgets = LinkedHashMap<String, Budget>(); val goals = LinkedHashMap<Long, Goal>(); val bills = LinkedHashMap<Long, Bill>()
    val payments = LinkedHashMap<Pair<Long,String>, BillPayment>(); val incomes = LinkedHashMap<Long, Income>()
    val oneTime = LinkedHashMap<Long, OneTimeIncome>(); val rules = LinkedHashMap<String, Rule>(); val metas = LinkedHashMap<String, TxMeta>()
    val netItems = LinkedHashMap<Long, NetItem>(); val envs = LinkedHashMap<String, Envelope>(); val accounts = LinkedHashMap<String, Account>()
    val txs = LinkedHashMap<String, Tx>(); val snaps = LinkedHashMap<String, Snapshot>(); val netSnaps = LinkedHashMap<String, NetSnapshot>()
    private var next = 1L
    private fun nid(id: Long) = if (id == 0L) next++ else { if (id >= next) next = id + 1; id }

    val daoImpl: FinDao = Proxy.newProxyInstance(FinDao::class.java.classLoader, arrayOf(FinDao::class.java)) { _, m, a ->
        val n = m.name
        when (n) {
            "budgetsNow" -> budgets.values.toList(); "goalsNow" -> goals.values.toList(); "billsNow" -> bills.values.toList()
            "billPaymentsNow" -> payments.values.toList(); "incomesNow" -> incomes.values.toList(); "oneTimeIncomesNow" -> oneTime.values.toList()
            "rulesNow" -> rules.values.toList(); "metasNow" -> metas.values.toList(); "netItemsNow" -> netItems.values.toList()
            "envelopesNow" -> envs.values.toList(); "cashAccountsNow" -> accounts.values.filter { it.itemId == "manual" }
            "manualTxNow" -> txs.values.filter { it.accountId == "manual" || it.accountId.startsWith("cash:") }
            "snapshotsNow" -> snaps.values.toList(); "netSnapshotsNow" -> netSnaps.values.toList()
            "putBudget" -> { val b = a[0] as Budget; budgets[b.category] = b; Unit }
            "putGoal" -> { val g = a[0] as Goal; val id = nid(g.id); goals[id] = g.copy(id = id); Unit }
            "putBill" -> { val b = a[0] as Bill; val id = nid(b.id); bills[id] = b.copy(id = id); id }
            "putBillPayment" -> { val p = a[0] as BillPayment; payments[p.billId to p.month] = p; Unit }
            "putIncome" -> { val i = a[0] as Income; val id = nid(i.id); incomes[id] = i.copy(id = id); Unit }
            "putOneTimeIncome" -> { val i = a[0] as OneTimeIncome; val id = nid(i.id); oneTime[id] = i.copy(id = id); Unit }
            "putRule" -> { val r = a[0] as Rule; rules[r.ruleKey] = r; Unit }
            "putMeta" -> { val r = a[0] as TxMeta; metas[r.txId] = r; Unit }
            "putNetItem" -> { val i = a[0] as NetItem; val id = nid(i.id); netItems[id] = i.copy(id = id); Unit }
            "putEnvelope" -> { val e = a[0] as Envelope; envs[e.category] = e; Unit }
            "putAccount" -> { val x = a[0] as Account; accounts[x.id] = x; Unit }
            "putTx" -> { val t = a[0] as Tx; txs[t.id] = t; Unit }
            "putSnapshot" -> { val s = a[0] as Snapshot; snaps[s.date] = s; Unit }
            "putNetSnapshot" -> { val s = a[0] as NetSnapshot; netSnaps[s.date] = s; Unit }
            "clearBudgets" -> { budgets.clear(); Unit }; "clearGoals" -> { goals.clear(); Unit }
            "clearBills" -> { bills.clear(); Unit }; "clearBillPayments" -> { payments.clear(); Unit }
            "clearIncomes" -> { incomes.clear(); Unit }; "clearOneTimeIncomes" -> { oneTime.clear(); Unit }
            "clearRules" -> { rules.clear(); Unit }; "clearMetas" -> { metas.clear(); Unit }
            "clearNetItems" -> { netItems.clear(); Unit }; "clearEnvelopes" -> { envs.clear(); Unit }
            "clearCashAccounts" -> { accounts.values.removeIf { it.itemId == "manual" }; Unit }
            "clearManualTx" -> { txs.values.removeIf { it.accountId == "manual" || it.accountId.startsWith("cash:") }; Unit }
            "clearSnapshots" -> { snaps.clear(); Unit }; "clearNetSnapshots" -> { netSnaps.clear(); Unit }
            else -> throw UnsupportedOperationException(n)
        }
    } as FinDao
    val dao: FinDao get() = daoImpl
}

