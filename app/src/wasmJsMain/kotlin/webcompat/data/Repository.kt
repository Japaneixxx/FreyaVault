package com.japaneixxx.freya.data

import com.japaneixxx.freya.web.WebBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

class Repository(private val settings: Settings, private val db: Db) {
    private val dao = db.dao()

    private class ItemData(
        val accounts: List<Account>,
        val txs: Map<String, List<Tx>>,
        val investments: List<Investment>?
    )

    suspend fun sync(): List<String> = withContext(Dispatchers.Default) {
        val api = PluggyApi(settings.clientId, settings.clientSecret)
        api.authenticate()
        val warnings = mutableListOf<String>()
        val itemUpdates = mutableListOf<Long>()
        val items = settings.itemList()
        var okCount = 0
        var firstError: Exception? = null

        for (itemId in items) {
            var institution = "Banco"
            try {
                runCatching { api.item(itemId) }.onSuccess { item ->
                    val name = item.optJSONObject("connector")?.str("name").orEmpty()
                    if (name.isNotBlank()) institution = name
                    val updated = item.str("lastUpdatedAt").ifBlank { item.str("updatedAt") }
                    runCatching { WebBridge.parseDateMillis(updated) }
                        .onSuccess { if (it > 0) itemUpdates += it }
                    when (val status = item.str("status")) {
                        "LOGIN_ERROR", "OUTDATED" -> warnings += "$institution: reconecte no Meu Pluggy ($status)"
                        "WAITING_USER_INPUT" -> warnings += "$institution: o banco pediu uma verificação (código/MFA) no Meu Pluggy"
                    }
                }
                val data = fetchItem(api, itemId, institution)
                writeItem(itemId, data)
                okCount++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (firstError == null) firstError = e
                warnings += "$institution: não sincronizou (${e.message ?: "erro"})"
            }
        }
        if (items.isNotEmpty() && okCount == 0) throw firstError ?: RuntimeException("Falha ao sincronizar")

        db.withTransaction {
            dao.deleteTxOfRemovedItems(items)
            dao.deleteAccountsOfRemovedItems(items)
            dao.deleteInvestmentsOfRemovedItems(items)
            val accounts = dao.accountsNow()
            val bank = bankTotal(accounts)
            val cards = cardsTotal(accounts)
            dao.putSnapshot(Snapshot(LocalDate.now().toString(), bank, cards))
            dao.recordNetSnapshot()
            settings.widgetBank = bank
            settings.widgetCards = cards
        }
        itemUpdates.minOrNull()?.let { settings.bankUpdatedAt = it }
        if (okCount > 0) settings.lastSync = System.currentTimeMillis()
        warnings
    }

    private suspend fun fetchItem(api: PluggyApi, itemId: String, institution: String): ItemData {
        val accounts = api.accounts(itemId).map { o ->
            val cd = o.optJSONObject("creditData")
            cd?.str("balanceCloseDate")?.take(10)?.takeIf { it.length == 10 }?.let { settings.putCardCloseDate(o.getString("id"), it) }
            Account(
                id = o.getString("id"),
                itemId = itemId,
                institution = institution,
                name = o.str("name").ifBlank { o.str("marketingName") }.ifBlank { "Conta" },
                type = o.str("type"),
                subtype = o.str("subtype"),
                balance = o.optDouble("balance", 0.0),
                currency = o.str("currencyCode").ifBlank { "BRL" },
                creditLimit = cd?.takeIf { !it.isNull("creditLimit") }?.optDouble("creditLimit"),
                availableLimit = cd?.takeIf { !it.isNull("availableCreditLimit") }?.optDouble("availableCreditLimit"),
                dueDate = cd?.str("balanceDueDate")?.take(10)?.ifBlank { null }
            )
        }

        val txs = LinkedHashMap<String, List<Tx>>()
        for (acc in accounts) {
            val last = dao.lastDate(acc.id)
            val from = if (last == null) LocalDate.now().minusMonths(12)
            else runCatching { LocalDate.parse(last).minusDays(45) }
                .getOrDefault(LocalDate.now().minusMonths(12))

            txs[acc.id] = api.transactions(acc.id, from.toString()).map { o ->
                val raw = abs(o.optDouble("amount", 0.0))
                val signed = if (o.str("type") == "DEBIT") -raw else raw
                Tx(
                    id = o.getString("id"),
                    accountId = acc.id,
                    date = o.str("date").take(10),
                    description = o.str("description").ifBlank { o.str("descriptionRaw") }
                        .ifBlank { "Sem descrição" },
                    amount = signed,
                    pluggyCategory = o.str("category"),
                    pending = o.str("status") == "PENDING"
                )
            }
        }

        val investments = runCatching { api.investments(itemId) }.getOrNull()?.let { list ->
            list.filter { o -> o.str("status").let { it.isBlank() || it == "ACTIVE" } }.map { o ->
                Investment(
                    id = o.getString("id"),
                    itemId = itemId,
                    institution = institution,
                    name = o.str("name").ifBlank { "Investimento" },
                    type = o.str("type").ifBlank { "OTHER" },
                    balance = o.optDouble("balance", 0.0),
                    amountOriginal = o.dbl("amountOriginal"),
                    amountProfit = o.dbl("amountProfit"),
                    annualRate = o.dbl("annualRate"),
                    lastTwelveMonthsRate = o.dbl("lastTwelveMonthsRate"),
                    currency = o.str("currencyCode").ifBlank { "BRL" }
                )
            }
        }
        return ItemData(accounts, txs, investments)
    }

    private suspend fun writeItem(itemId: String, data: ItemData) = db.withTransaction {
        dao.upsertAccounts(data.accounts)
        if (data.accounts.isNotEmpty()) dao.deleteAccountsOfItemNotIn(itemId, data.accounts.map { it.id })
        for ((accountId, txs) in data.txs) {
            dao.deletePending(accountId)
            val res = dao.insertTx(txs)
            val existing = txs.filterIndexed { i, _ -> res[i] == -1L }
            if (existing.isNotEmpty()) dao.updateTx(existing)
        }
        data.investments?.let {
            dao.deleteInvestmentsOf(itemId)
            dao.upsertInvestments(it)
        }
    }
}
