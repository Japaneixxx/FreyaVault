package com.japaneixxx.freya.data

import android.content.Context
import com.japaneixxx.freya.web.WebBridge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class Account(
    val id: String,
    val itemId: String,
    val institution: String,
    val name: String,
    val type: String,
    val subtype: String,
    val balance: Double,
    val currency: String,
    val creditLimit: Double? = null,
    val availableLimit: Double? = null,
    val dueDate: String? = null
)

data class Tx(
    val id: String,
    val accountId: String,
    val date: String,
    val description: String,
    val amount: Double,
    val pluggyCategory: String,
    val pending: Boolean
)

data class Rule(val ruleKey: String, val category: String)
data class Budget(val category: String, val limitAmount: Double)
data class Goal(val id: Long = 0, val name: String, val target: Double, val saved: Double = 0.0)
data class TxMeta(val txId: String, val note: String = "", val ignored: Boolean = false)

data class Bill(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int,
    val lastPaidMonth: String = "",
    val kind: String? = null,
    val totalInstallments: Int? = null,
    val firstMonth: String? = null,
    val matchText: String? = null
)

data class BillPayment(val billId: Long, val month: String, val amount: Double)

data class Income(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val day: Int,
    val businessDay: Boolean = false
)

data class OneTimeIncome(val id: Long = 0, val name: String, val amount: Double, val date: String)
data class Snapshot(val date: String, val bank: Double, val cards: Double)

data class Investment(
    val id: String,
    val itemId: String,
    val institution: String,
    val name: String,
    val type: String,
    val balance: Double,
    val amountOriginal: Double? = null,
    val amountProfit: Double? = null,
    val annualRate: Double? = null,
    val lastTwelveMonthsRate: Double? = null,
    val currency: String = "BRL"
)

data class NetItem(val id: Long = 0, val name: String, val kind: String, val amount: Double)
data class Envelope(val category: String, val startMonth: String)
data class NetSnapshot(val date: String, val total: Double)

/** Tabela em memória, persistida como JSON no localStorage do navegador. */
private class Table<T>(
    private val key: String,
    private val encode: (T) -> JSONObject,
    private val decode: (JSONObject) -> T
) {
    private val state = MutableStateFlow(load())
    val flow: Flow<List<T>> get() = state
    val value: List<T> get() = state.value

    private fun load(): List<T> = runCatching {
        val raw = WebBridge.storageGet("freya.db.$key") ?: return@runCatching emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).map { decode(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun set(list: List<T>) {
        state.value = list
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(encode(it)) }
            WebBridge.storageSet("freya.db.$key", arr.toString())
        }
    }

    fun update(transform: (List<T>) -> List<T>) = set(transform(value))

    companion object {
        fun json(block: JSONObject.() -> Unit): JSONObject = JSONObject().apply(block)
        fun optDouble(o: JSONObject, key: String): Double? =
            if (!o.has(key) || o.isNull(key)) null else o.optDouble(key).takeUnless { it.isNaN() }
        fun optLong(o: JSONObject, key: String): Long? =
            if (!o.has(key) || o.isNull(key)) null else o.optLong(key)
        fun optInt(o: JSONObject, key: String): Int? =
            if (!o.has(key) || o.isNull(key)) null else o.optInt(key)
        fun putNullable(o: JSONObject, key: String, value: Any?) { if (value != null) o.put(key, value) }
    }
}

private fun accountJson(a: Account) = Table.json {
    put("id", a.id); put("itemId", a.itemId); put("institution", a.institution); put("name", a.name)
    put("type", a.type); put("subtype", a.subtype); put("balance", a.balance); put("currency", a.currency)
    Table.putNullable(this, "creditLimit", a.creditLimit)
    Table.putNullable(this, "availableLimit", a.availableLimit)
    Table.putNullable(this, "dueDate", a.dueDate)
}

private fun accountFrom(o: JSONObject) = Account(
    id = o.getString("id"), itemId = o.optString("itemId"), institution = o.optString("institution"),
    name = o.optString("name"), type = o.optString("type"), subtype = o.optString("subtype"),
    balance = o.optDouble("balance", 0.0), currency = o.optString("currency", "BRL"),
    creditLimit = Table.optDouble(o, "creditLimit"), availableLimit = Table.optDouble(o, "availableLimit"),
    dueDate = Table.optString(o, "dueDate")
)

private fun txJson(t: Tx) = Table.json {
    put("id", t.id); put("accountId", t.accountId); put("date", t.date); put("description", t.description)
    put("amount", t.amount); put("pluggyCategory", t.pluggyCategory); put("pending", t.pending)
}

private fun txFrom(o: JSONObject) = Tx(
    id = o.getString("id"), accountId = o.optString("accountId"), date = o.optString("date"),
    description = o.optString("description"), amount = o.optDouble("amount", 0.0),
    pluggyCategory = o.optString("pluggyCategory"), pending = o.optBoolean("pending")
)

private fun billJson(b: Bill) = Table.json {
    put("id", b.id); put("name", b.name); put("amount", b.amount); put("dueDay", b.dueDay)
    put("lastPaidMonth", b.lastPaidMonth)
    Table.putNullable(this, "kind", b.kind)
    Table.putNullable(this, "totalInstallments", b.totalInstallments)
    Table.putNullable(this, "firstMonth", b.firstMonth)
    Table.putNullable(this, "matchText", b.matchText)
}

private fun billFrom(o: JSONObject) = Bill(
    id = o.optLong("id"), name = o.optString("name"), amount = o.optDouble("amount", 0.0),
    dueDay = o.optInt("dueDay"), lastPaidMonth = o.optString("lastPaidMonth"),
    kind = Table.optString(o, "kind").ifBlank { null },
    totalInstallments = Table.optInt(o, "totalInstallments"),
    firstMonth = Table.optString(o, "firstMonth").ifBlank { null },
    matchText = Table.optString(o, "matchText").ifBlank { null }
)

private fun investmentJson(i: Investment) = Table.json {
    put("id", i.id); put("itemId", i.itemId); put("institution", i.institution); put("name", i.name)
    put("type", i.type); put("balance", i.balance); put("currency", i.currency)
    Table.putNullable(this, "amountOriginal", i.amountOriginal)
    Table.putNullable(this, "amountProfit", i.amountProfit)
    Table.putNullable(this, "annualRate", i.annualRate)
    Table.putNullable(this, "lastTwelveMonthsRate", i.lastTwelveMonthsRate)
}

private fun investmentFrom(o: JSONObject) = Investment(
    id = o.getString("id"), itemId = o.optString("itemId"), institution = o.optString("institution"),
    name = o.optString("name"), type = o.optString("type"), balance = o.optDouble("balance", 0.0),
    amountOriginal = Table.optDouble(o, "amountOriginal"), amountProfit = Table.optDouble(o, "amountProfit"),
    annualRate = Table.optDouble(o, "annualRate"), lastTwelveMonthsRate = Table.optDouble(o, "lastTwelveMonthsRate"),
    currency = o.optString("currency", "BRL")
)

private fun Table.Companion.optString(o: JSONObject, key: String): String =
    if (!o.has(key) || o.isNull(key)) "" else o.optString(key)

/** DAO local com a mesma API do Room usada pela interface e pelo backup. */
class FinDao internal constructor() {
    private val accounts = Table("accounts", ::accountJson, ::accountFrom)
    private val txs = Table("transactions", ::txJson, ::txFrom)
    private val rules = Table("rules", { Table.json { put("ruleKey", it.ruleKey); put("category", it.category) } },
        { Rule(it.optString("ruleKey"), it.optString("category")) })
    private val budgets = Table("budgets", { Table.json { put("category", it.category); put("limitAmount", it.limitAmount) } },
        { Budget(it.optString("category"), it.optDouble("limitAmount", 0.0)) })
    private val goals = Table("goals", { Table.json {
        put("id", it.id); put("name", it.name); put("target", it.target); put("saved", it.saved)
    } }, { Goal(it.optLong("id"), it.optString("name"), it.optDouble("target", 0.0), it.optDouble("saved", 0.0)) })
    private val metas = Table("tx_meta", { Table.json {
        put("txId", it.txId); put("note", it.note); put("ignored", it.ignored)
    } }, { TxMeta(it.optString("txId"), it.optString("note"), it.optBoolean("ignored")) })
    private val bills = Table("bills", ::billJson, ::billFrom)
    private val payments = Table("bill_payments", { Table.json {
        put("billId", it.billId); put("month", it.month); put("amount", it.amount)
    } }, { BillPayment(it.optLong("billId"), it.optString("month"), it.optDouble("amount", 0.0)) })
    private val incomes = Table("incomes", { Table.json {
        put("id", it.id); put("name", it.name); put("amount", it.amount); put("day", it.day); put("businessDay", it.businessDay)
    } }, { Income(it.optLong("id"), it.optString("name"), it.optDouble("amount", 0.0), it.optInt("day"), it.optBoolean("businessDay")) })
    private val oneTimeIncomes = Table("one_time_incomes", { Table.json {
        put("id", it.id); put("name", it.name); put("amount", it.amount); put("date", it.date)
    } }, { OneTimeIncome(it.optLong("id"), it.optString("name"), it.optDouble("amount", 0.0), it.optString("date")) })
    private val snapshots = Table("snapshots", { Table.json {
        put("date", it.date); put("bank", it.bank); put("cards", it.cards)
    } }, { Snapshot(it.optString("date"), it.optDouble("bank", 0.0), it.optDouble("cards", 0.0)) })
    private val investments = Table("investments", ::investmentJson, ::investmentFrom)
    private val netItems = Table("net_items", { Table.json {
        put("id", it.id); put("name", it.name); put("kind", it.kind); put("amount", it.amount)
    } }, { NetItem(it.optLong("id"), it.optString("name"), it.optString("kind"), it.optDouble("amount", 0.0)) })
    private val envelopes = Table("envelopes", { Table.json {
        put("category", it.category); put("startMonth", it.startMonth)
    } }, { Envelope(it.optString("category"), it.optString("startMonth")) })
    private val netSnapshots = Table("net_snapshots", { Table.json {
        put("date", it.date); put("total", it.total)
    } }, { NetSnapshot(it.optString("date"), it.optDouble("total", 0.0)) })

    private fun nextId(current: List<Long>): Long = (current.maxOrNull() ?: 0L) + 1L

    // ---- leituras reativas ----
    fun accounts(): Flow<List<Account>> = accounts.flow.map { it.sortedWith(compareBy({ it.institution }, { it.name })) }
    fun transactions(): Flow<List<Tx>> = txs.flow.map { it.sortedWith(compareByDescending<Tx> { it.date }.thenBy { it.id }) }
    fun rules(): Flow<List<Rule>> = rules.flow
    fun budgets(): Flow<List<Budget>> = budgets.flow
    fun goals(): Flow<List<Goal>> = goals.flow.map { it.sortedBy { g -> g.id } }
    fun metas(): Flow<List<TxMeta>> = metas.flow
    fun bills(): Flow<List<Bill>> = bills.flow.map { it.sortedWith(compareBy({ it.dueDay }, { it.name })) }
    fun snapshots(): Flow<List<Snapshot>> = snapshots.flow.map { it.sortedBy { s -> s.date } }
    fun investments(): Flow<List<Investment>> = investments.flow.map { it.sortedByDescending { i -> i.balance } }
    fun netItems(): Flow<List<NetItem>> = netItems.flow.map { it.sortedWith(compareBy({ it.kind }, { it.name })) }
    fun envelopes(): Flow<List<Envelope>> = envelopes.flow
    fun netSnapshots(): Flow<List<NetSnapshot>> = netSnapshots.flow.map { it.sortedBy { s -> s.date } }
    fun billPayments(): Flow<List<BillPayment>> = payments.flow
    fun incomes(): Flow<List<Income>> = incomes.flow.map { it.sortedWith(compareBy({ it.day }, { it.name })) }
    fun oneTimeIncomes(): Flow<List<OneTimeIncome>> = oneTimeIncomes.flow.map { it.sortedWith(compareBy({ it.date }, { it.name })) }

    // ---- leituras pontuais ----
    suspend fun accountsNow(): List<Account> = accounts.value
    suspend fun txNow(): List<Tx> = txs.value
    suspend fun manualTxNow(): List<Tx> = txs.value.filter { isManualAccountId(it.accountId) }
    suspend fun cashAccountsNow(): List<Account> = accounts.value.filter { it.itemId == MANUAL_ITEM_ID }
    suspend fun snapshotsNow(): List<Snapshot> = snapshots.value
    suspend fun netSnapshotsNow(): List<NetSnapshot> = netSnapshots.value
    suspend fun rulesNow(): List<Rule> = rules.value
    suspend fun budgetsNow(): List<Budget> = budgets.value
    suspend fun goalsNow(): List<Goal> = goals.value
    suspend fun metasNow(): List<TxMeta> = metas.value
    suspend fun billsNow(): List<Bill> = bills.value
    suspend fun investmentsNow(): List<Investment> = investments.value
    suspend fun netItemsNow(): List<NetItem> = netItems.value
    suspend fun envelopesNow(): List<Envelope> = envelopes.value
    suspend fun billPaymentsNow(): List<BillPayment> = payments.value
    suspend fun incomesNow(): List<Income> = incomes.value
    suspend fun oneTimeIncomesNow(): List<OneTimeIncome> = oneTimeIncomes.value
    suspend fun txById(id: String): Tx? = txs.value.firstOrNull { it.id == id }
    suspend fun lastDate(accountId: String): String? =
        txs.value.filter { it.accountId == accountId }.maxByOrNull { it.date }?.date

    // ---- escritas ----
    suspend fun upsertAccounts(list: List<Account>) = accounts.update { current ->
        current.filterNot { a -> a.id in list.map { it.id }.toSet() } + list
    }
    suspend fun putAccount(a: Account) = accounts.update { it.filterNot { x -> x.id == a.id } + a }
    suspend fun insertTx(list: List<Tx>): List<Long> {
        val existing = txs.value.map { it.id }.toSet()
        val results = ArrayList<Long>(list.size)
        val toAdd = ArrayList<Tx>()
        list.forEach { tx ->
            if (tx.id in existing) results.add(-1L) else { toAdd.add(tx); results.add((txs.value.size + toAdd.size).toLong()) }
        }
        if (toAdd.isNotEmpty()) txs.update { it + toAdd }
        return results
    }
    suspend fun updateTx(list: List<Tx>) = txs.update { current ->
        val ids = list.map { it.id }.toSet()
        current.map { if (it.id in ids) list.first { x -> x.id == it.id } else it }
    }
    suspend fun putTx(tx: Tx) = txs.update { it.filterNot { x -> x.id == tx.id } + tx }
    suspend fun putRule(rule: Rule) = rules.update { it.filterNot { x -> x.ruleKey == rule.ruleKey } + rule }
    suspend fun putBudget(b: Budget) = budgets.update { it.filterNot { x -> x.category == b.category } + b }
    suspend fun putGoal(g: Goal) = goals.update { current ->
        val item = if (g.id == 0L) g.copy(id = nextId(current.map { it.id })) else g
        current.filterNot { it.id == item.id } + item
    }
    suspend fun putMeta(m: TxMeta) = metas.update { it.filterNot { x -> x.txId == m.txId } + m }
    suspend fun putBill(b: Bill): Long {
        val id = if (b.id == 0L) nextId(bills.value.map { it.id }) else b.id
        bills.update { it.filterNot { x -> x.id == id } + b.copy(id = id) }
        return id
    }
    suspend fun putSnapshot(s: Snapshot) = snapshots.update { it.filterNot { x -> x.date == s.date } + s }
    suspend fun upsertInvestments(list: List<Investment>) = investments.update { current ->
        val ids = list.map { it.id }.toSet()
        current.filterNot { it.id in ids } + list
    }
    suspend fun putNetItem(i: NetItem) = netItems.update { current ->
        val item = if (i.id == 0L) i.copy(id = nextId(current.map { it.id })) else i
        current.filterNot { it.id == item.id } + item
    }
    suspend fun putEnvelope(e: Envelope) = envelopes.update { it.filterNot { x -> x.category == e.category } + e }
    suspend fun putNetSnapshot(s: NetSnapshot) = netSnapshots.update { it.filterNot { x -> x.date == s.date } + s }
    suspend fun putBillPayment(p: BillPayment) = payments.update {
        it.filterNot { x -> x.billId == p.billId && x.month == p.month } + p
    }
    suspend fun putIncome(i: Income) = incomes.update { current ->
        val item = if (i.id == 0L) i.copy(id = nextId(current.map { it.id })) else i
        current.filterNot { it.id == item.id } + item
    }
    suspend fun putOneTimeIncome(i: OneTimeIncome) = oneTimeIncomes.update { current ->
        val item = if (i.id == 0L) i.copy(id = nextId(current.map { it.id })) else i
        current.filterNot { it.id == item.id } + item
    }

    suspend fun deleteInvestmentsOf(itemId: String) = investments.update { it.filterNot { i -> i.itemId == itemId } }
    suspend fun deleteNetItem(id: Long) = netItems.update { it.filterNot { i -> i.id == id } }
    suspend fun deleteEnvelope(c: String) = envelopes.update { it.filterNot { e -> e.category == c } }
    suspend fun deleteBillPayment(billId: Long, month: String) = payments.update {
        it.filterNot { p -> p.billId == billId && p.month == month }
    }
    suspend fun deletePaymentsOf(billId: Long) = payments.update { it.filterNot { p -> p.billId == billId } }
    suspend fun deleteIncome(id: Long) = incomes.update { it.filterNot { i -> i.id == id } }
    suspend fun deleteOneTimeIncome(id: Long) = oneTimeIncomes.update { it.filterNot { i -> i.id == id } }
    suspend fun clearBillPayments() = payments.set(emptyList())
    suspend fun clearIncomes() = incomes.set(emptyList())
    suspend fun clearOneTimeIncomes() = oneTimeIncomes.set(emptyList())
    suspend fun clearInvestments() = investments.set(emptyList())
    suspend fun clearNetItems() = netItems.set(emptyList())
    suspend fun clearEnvelopes() = envelopes.set(emptyList())
    suspend fun clearNetSnapshots() = netSnapshots.set(emptyList())

    suspend fun deletePending(accountId: String) = txs.update { it.filterNot { t -> t.accountId == accountId && t.pending } }
    suspend fun deleteTx(id: String) = txs.update { it.filterNot { t -> t.id == id } }
    suspend fun deleteRule(key: String) = rules.update { it.filterNot { r -> r.ruleKey == key } }
    suspend fun deleteBudget(c: String) = budgets.update { it.filterNot { b -> b.category == c } }
    suspend fun deleteGoal(id: Long) = goals.update { it.filterNot { g -> g.id == id } }
    suspend fun deleteBill(id: Long) = bills.update { it.filterNot { b -> b.id == id } }

    suspend fun addToBalance(id: String, delta: Double) = accounts.update {
        it.map { a -> if (a.id == id) a.copy(balance = (a.balance + delta).r2()) else a }
    }
    suspend fun setBalance(id: String, balance: Double) = accounts.update {
        it.map { a -> if (a.id == id) a.copy(balance = balance) else a }
    }
    suspend fun deleteAccount(id: String) = accounts.update { it.filterNot { a -> a.id == id } }
    suspend fun deleteTxOfAccount(accountId: String) = txs.update { it.filterNot { t -> t.accountId == accountId } }
    suspend fun clearCashAccounts() = accounts.update { it.filterNot { a -> a.itemId == MANUAL_ITEM_ID } }
    suspend fun clearManualTx() = txs.update { it.filterNot { t -> isManualAccountId(t.accountId) } }

    suspend fun deleteAccountsOfRemovedItems(itemIds: List<String>) = accounts.update {
        it.filterNot { a -> a.itemId != MANUAL_ITEM_ID && a.itemId !in itemIds }
    }
    suspend fun deleteInvestmentsOfRemovedItems(itemIds: List<String>) = investments.update {
        it.filterNot { i -> i.itemId !in itemIds }
    }
    suspend fun deleteTxOfRemovedItems(itemIds: List<String>) {
        val removedIds = accounts.value.filter { it.itemId != MANUAL_ITEM_ID && it.itemId !in itemIds }.map { it.id }.toSet()
        txs.update { it.filterNot { t -> t.accountId in removedIds } }
    }
    suspend fun deleteAccountsOfItemNotIn(itemId: String, keep: List<String>) = accounts.update {
        it.filterNot { a -> a.itemId == itemId && a.id !in keep }
    }

    suspend fun clearAccounts() = accounts.set(emptyList())
    suspend fun clearTx() = txs.set(emptyList())
    suspend fun clearRules() = rules.set(emptyList())
    suspend fun clearBudgets() = budgets.set(emptyList())
    suspend fun clearGoals() = goals.set(emptyList())
    suspend fun clearMetas() = metas.set(emptyList())
    suspend fun clearBills() = bills.set(emptyList())
    suspend fun clearSnapshots() = snapshots.set(emptyList())
}

class Db private constructor() {
    private val finDao: FinDao = FinDao()

    fun dao(): FinDao = finDao

    suspend fun <T> withTransaction(block: suspend () -> T): T = block()

    companion object {
        private var instance: Db? = null
        fun get(ctx: Context): Db = instance ?: Db().also { instance = it }
    }
}
