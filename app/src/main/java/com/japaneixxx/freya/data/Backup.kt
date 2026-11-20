package com.japaneixxx.freya.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup em JSON do que NÃO vem do banco: orçamentos, metas, contas a pagar, regras, notas, lançamentos manuais,
 * contas de dinheiro físico, histórico dos gráficos, categorias próprias e nomes de contas.
 */
object Backup {
    /** Versão do formato. 1 = antigo (sem histórico, categorias e contas de dinheiro). */
    const val VERSION = 2

    private suspend fun JSONArray?.each(f: suspend (JSONObject) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) f(getJSONObject(i))
    }

    suspend fun export(
        dao: FinDao,
        accountAliases: Map<String, String> = emptyMap(),
        customCategories: List<String> = emptyList()
    ): String {
        val o = JSONObject().put("version", VERSION).put("exportedAt", System.currentTimeMillis())
        o.put("budgets", JSONArray().also { a ->
            dao.budgetsNow().forEach { a.put(JSONObject().put("category", it.category).put("limit", it.limitAmount)) }
        })
        o.put("goals", JSONArray().also { a ->
            dao.goalsNow().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("target", it.target).put("saved", it.saved))
            }
        })
        o.put("bills", JSONArray().also { a ->
            dao.billsNow().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount)
                    .put("dueDay", it.dueDay).put("paid", it.lastPaidMonth)
                    .put("kind", it.kind ?: "").put("total", it.totalInstallments ?: 0)
                    .put("first", it.firstMonth ?: "").put("match", it.matchText ?: ""))
            }
        })
        o.put("billPayments", JSONArray().also { a ->
            dao.billPaymentsNow().forEach { a.put(JSONObject().put("billId", it.billId).put("month", it.month).put("amount", it.amount)) }
        })
        o.put("incomes", JSONArray().also { a ->
            dao.incomesNow().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount)
                    .put("day", it.day).put("businessDay", it.businessDay))
            }
        })
        o.put("oneTimeIncomes", JSONArray().also { a ->
            dao.oneTimeIncomesNow().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("amount", it.amount).put("date", it.date))
            }
        })
        o.put("rules", JSONArray().also { a ->
            dao.rulesNow().forEach { a.put(JSONObject().put("key", it.ruleKey).put("category", it.category)) }
        })
        o.put("metas", JSONArray().also { a ->
            dao.metasNow().forEach { a.put(JSONObject().put("txId", it.txId).put("note", it.note).put("ignored", it.ignored)) }
        })
        o.put("netItems", JSONArray().also { a ->
            dao.netItemsNow().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind).put("amount", it.amount))
            }
        })
        o.put("envelopes", JSONArray().also { a ->
            dao.envelopesNow().forEach { a.put(JSONObject().put("category", it.category).put("start", it.startMonth)) }
        })
        o.put("cashAccounts", JSONArray().also { a ->
            dao.cashAccountsNow().forEach {
                a.put(JSONObject().put("id", it.id).put("institution", it.institution).put("name", it.name)
                    .put("balance", it.balance).put("currency", it.currency))
            }
        })
        o.put("manual", JSONArray().also { a ->
            dao.manualTxNow().forEach {
                a.put(JSONObject().put("id", it.id).put("accountId", it.accountId).put("date", it.date)
                    .put("description", it.description).put("amount", it.amount).put("category", it.pluggyCategory))
            }
        })
        o.put("snapshots", JSONArray().also { a ->
            dao.snapshotsNow().forEach { a.put(JSONObject().put("date", it.date).put("bank", it.bank).put("cards", it.cards)) }
        })
        o.put("netSnapshots", JSONArray().also { a ->
            dao.netSnapshotsNow().forEach { a.put(JSONObject().put("date", it.date).put("total", it.total)) }
        })
        o.put("customCategories", JSONArray().also { a -> customCategories.forEach { a.put(it) } })
        o.put("accountAliases", JSONObject().also { a -> accountAliases.forEach { (id, name) -> a.put(id, name) } })
        return o.toString(2)
    }

    /**
     * Restaura o backup e devolve quantos itens foram aplicados. Tudo numa transação: se algo falhar, nada muda.
     * [replace] = true troca os dados que o backup traz pelos dele (restauração clássica);
     * false mescla: só adiciona o que ainda não existe, sem sobrescrever nada e sem duplicar.
     */
    suspend fun import(
        dao: FinDao,
        inTransaction: suspend (suspend () -> Unit) -> Unit,
        json: String,
        replace: Boolean = false,
        onAccountAliases: (Map<String, String>) -> Unit = {},
        onCustomCategories: (List<String>) -> Unit = {}
    ): Int {
        val o = JSONObject(json)
        val version = o.optInt("version", 1)
        require(version <= VERSION) {
            "Este backup foi criado por uma versão mais nova do Freya (formato $version). Atualize o app para restaurá-lo."
        }
        var n = 0

        inTransaction {
            if (replace) {
                if (o.has("budgets")) dao.clearBudgets()
                if (o.has("goals")) dao.clearGoals()
                if (o.has("bills")) { dao.clearBills(); dao.clearBillPayments() }
                if (o.has("billPayments")) dao.clearBillPayments()
                if (o.has("incomes")) dao.clearIncomes()
                if (o.has("oneTimeIncomes")) dao.clearOneTimeIncomes()
                if (o.has("rules")) dao.clearRules()
                if (o.has("metas")) dao.clearMetas()
                if (o.has("netItems")) dao.clearNetItems()
                if (o.has("envelopes")) dao.clearEnvelopes()
                if (o.has("cashAccounts")) dao.clearCashAccounts()
                if (o.has("manual")) dao.clearManualTx()
                if (o.has("snapshots")) dao.clearSnapshots()
                if (o.has("netSnapshots")) dao.clearNetSnapshots()
            }

            // O que já existe no aparelho (vazio após o "replace"); no modo mesclar evita sobrescrever e duplicar.
            val haveBudgets = dao.budgetsNow().map { it.category }.toSet()
            val haveGoals = dao.goalsNow().associate { (it.name to it.target) to it.id }
            val haveBills = dao.billsNow().associate { Triple(it.name, it.amount, it.dueDay) to it.id }
            val haveIncomes = dao.incomesNow().map { Triple(it.name, it.amount, it.day) }.toSet()
            val haveOneTime = dao.oneTimeIncomesNow().map { Triple(it.name, it.amount, it.date) }.toSet()
            val haveNet = dao.netItemsNow().map { Triple(it.name, it.kind, it.amount) }.toSet()
            val haveRules = dao.rulesNow().map { it.ruleKey }.toSet()
            val haveMetas = dao.metasNow().map { it.txId }.toSet()
            val haveEnv = dao.envelopesNow().map { it.category }.toSet()
            val havePayments = dao.billPaymentsNow().map { it.billId to it.month }.toSet()
            val haveTx = dao.manualTxNow().map { it.id }.toSet()
            val haveCash = dao.cashAccountsNow().map { it.id }.toSet()
            val haveSnap = dao.snapshotsNow().map { it.date }.toSet()
            val haveNetSnap = dao.netSnapshotsNow().map { it.date }.toSet()

            o.optJSONArray("budgets").each {
                if (replace || it.getString("category") !in haveBudgets) {
                    dao.putBudget(Budget(it.getString("category"), it.getDouble("limit"))); n++
                }
            }
            o.optJSONArray("goals").each {
                val name = it.getString("name"); val target = it.getDouble("target")
                if (replace) { dao.putGoal(Goal(it.optLong("id"), name, target, it.optDouble("saved", 0.0))); n++ }
                else if ((name to target) !in haveGoals) { dao.putGoal(Goal(0, name, target, it.optDouble("saved", 0.0))); n++ }
            }

            // Contas a pagar: no modo mesclar os IDs mudam, então os pagamentos precisam seguir o novo ID.
            val billIdMap = HashMap<Long, Long>()
            o.optJSONArray("bills").each {
                val oldId = it.optLong("id")
                val bill = Bill(
                    if (replace) oldId else 0, it.getString("name"), it.getDouble("amount"), it.getInt("dueDay"), it.str("paid"),
                    it.str("kind").ifBlank { null },
                    it.optInt("total", 0).takeIf { t -> t > 0 },
                    it.str("first").ifBlank { null },
                    it.str("match").ifBlank { null }
                )
                if (replace) { dao.putBill(bill); billIdMap[oldId] = oldId; n++ }
                else {
                    val same = haveBills[Triple(bill.name, bill.amount, bill.dueDay)]
                    if (same != null) billIdMap[oldId] = same
                    else { billIdMap[oldId] = dao.putBill(bill); n++ }
                }
            }
            o.optJSONArray("billPayments").each {
                val newId = billIdMap[it.getLong("billId")] ?: return@each
                val month = it.getString("month")
                if (replace || (newId to month) !in havePayments) {
                    dao.putBillPayment(BillPayment(newId, month, it.getDouble("amount"))); n++
                }
            }

            o.optJSONArray("incomes").each {
                val key = Triple(it.getString("name"), it.getDouble("amount"), it.getInt("day"))
                if (replace || key !in haveIncomes) {
                    dao.putIncome(Income(if (replace) it.optLong("id") else 0, key.first, key.second, key.third, it.optBoolean("businessDay"))); n++
                }
            }
            o.optJSONArray("oneTimeIncomes").each {
                val key = Triple(it.getString("name"), it.getDouble("amount"), it.getString("date"))
                if (replace || key !in haveOneTime) {
                    dao.putOneTimeIncome(OneTimeIncome(if (replace) it.optLong("id") else 0, key.first, key.second, key.third)); n++
                }
            }
            o.optJSONArray("rules").each {
                if (replace || it.getString("key") !in haveRules) { dao.putRule(Rule(it.getString("key"), it.getString("category"))); n++ }
            }
            o.optJSONArray("metas").each {
                if (replace || it.getString("txId") !in haveMetas) {
                    dao.putMeta(TxMeta(it.getString("txId"), it.str("note"), it.optBoolean("ignored"))); n++
                }
            }
            o.optJSONArray("netItems").each {
                val key = Triple(it.getString("name"), it.getString("kind"), it.getDouble("amount"))
                if (replace || key !in haveNet) {
                    dao.putNetItem(NetItem(if (replace) it.optLong("id") else 0, key.first, key.second, key.third)); n++
                }
            }
            o.optJSONArray("envelopes").each {
                if (replace || it.getString("category") !in haveEnv) { dao.putEnvelope(Envelope(it.getString("category"), it.getString("start"))); n++ }
            }

            // Contas de dinheiro: no modo mesclar só entram as que não existem (junto com os lançamentos delas).
            val addedCash = HashSet<String>()
            o.optJSONArray("cashAccounts").each {
                val id = it.getString("id")
                if (replace || id !in haveCash) {
                    dao.putAccount(
                        Account(id, MANUAL_ITEM_ID, it.str("institution").ifBlank { "Dinheiro" }, it.str("name").ifBlank { "Carteira" },
                            "BANK", "CASH", it.optDouble("balance", 0.0), it.str("currency").ifBlank { "BRL" })
                    )
                    addedCash += id; n++
                }
            }
            o.optJSONArray("manual").each {
                val id = it.getString("id")
                val accountId = it.str("accountId").ifBlank { MANUAL_ACCOUNT_ID }
                val allowed = if (isCashAccountId(accountId)) (replace || accountId in addedCash) else true
                if (allowed && (replace || id !in haveTx)) {
                    dao.putTx(Tx(id, accountId, it.getString("date"), it.getString("description"),
                        it.getDouble("amount"), it.str("category"), false)); n++
                }
            }
            o.optJSONArray("snapshots").each {
                if (replace || it.getString("date") !in haveSnap) {
                    dao.putSnapshot(Snapshot(it.getString("date"), it.getDouble("bank"), it.getDouble("cards"))); n++
                }
            }
            o.optJSONArray("netSnapshots").each {
                if (replace || it.getString("date") !in haveNetSnap) {
                    dao.putNetSnapshot(NetSnapshot(it.getString("date"), it.getDouble("total"))); n++
                }
            }
        }

        // Configurações (fora do banco). Backups antigos não têm estes campos: nesse caso nada é alterado.
        o.optJSONObject("accountAliases")?.let { a ->
            val map = a.keys().asSequence().map { it.toString() }.associateWith { k -> a.optString(k) }.filterValues { it.isNotBlank() }
            onAccountAliases(map)
            n += map.size
        }
        o.optJSONArray("customCategories")?.let { a ->
            val list = (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
            onCustomCategories(list)
            n += list.size
        }
        return n
    }
}
