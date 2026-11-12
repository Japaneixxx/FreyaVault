package app.meufin.data

import org.json.JSONArray
import org.json.JSONObject

/** Backup em JSON do que NÃO vem do banco: orçamentos, metas, contas a pagar, regras, notas e lançamentos manuais. */
object Backup {
    private suspend fun JSONArray?.each(f: suspend (JSONObject) -> Unit) {
        if (this == null) return
        for (i in 0 until length()) f(getJSONObject(i))
    }

    suspend fun export(dao: FinDao): String {
        val o = JSONObject().put("version", 1)
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
        o.put("manual", JSONArray().also { a ->
            dao.manualTxNow().forEach {
                a.put(JSONObject().put("id", it.id).put("date", it.date).put("description", it.description)
                    .put("amount", it.amount).put("category", it.pluggyCategory))
            }
        })
        return o.toString(2)
    }

    /** Restaura (mescla) o backup e devolve quantos itens foram aplicados. */
    suspend fun import(dao: FinDao, json: String): Int {
        val o = JSONObject(json)
        var n = 0
        o.optJSONArray("budgets").each { dao.putBudget(Budget(it.getString("category"), it.getDouble("limit"))); n++ }
        o.optJSONArray("goals").each {
            dao.putGoal(Goal(it.optLong("id"), it.getString("name"), it.getDouble("target"), it.optDouble("saved", 0.0))); n++
        }
        o.optJSONArray("bills").each {
            dao.putBill(
                Bill(
                    it.optLong("id"), it.getString("name"), it.getDouble("amount"), it.getInt("dueDay"), it.str("paid"),
                    it.str("kind").ifBlank { null },
                    it.optInt("total", 0).takeIf { t -> t > 0 },
                    it.str("first").ifBlank { null },
                    it.str("match").ifBlank { null }
                )
            ); n++
        }
        o.optJSONArray("billPayments").each { dao.putBillPayment(BillPayment(it.getLong("billId"), it.getString("month"), it.getDouble("amount"))); n++ }
        o.optJSONArray("incomes").each {
            dao.putIncome(Income(it.optLong("id"), it.getString("name"), it.getDouble("amount"), it.getInt("day"), it.optBoolean("businessDay"))); n++
        }
        o.optJSONArray("rules").each { dao.putRule(Rule(it.getString("key"), it.getString("category"))); n++ }
        o.optJSONArray("metas").each {
            dao.putMeta(TxMeta(it.getString("txId"), it.str("note"), it.optBoolean("ignored"))); n++
        }
        o.optJSONArray("netItems").each {
            dao.putNetItem(NetItem(it.optLong("id"), it.getString("name"), it.getString("kind"), it.getDouble("amount"))); n++
        }
        o.optJSONArray("envelopes").each { dao.putEnvelope(Envelope(it.getString("category"), it.getString("start"))); n++ }
        o.optJSONArray("manual").each {
            dao.putTx(Tx(it.getString("id"), "manual", it.getString("date"), it.getString("description"),
                it.getDouble("amount"), it.str("category"), false)); n++
        }
        return n
    }
}
