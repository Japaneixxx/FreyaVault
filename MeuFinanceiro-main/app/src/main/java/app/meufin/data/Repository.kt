package app.meufin.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

class Repository(private val settings: Settings, private val dao: FinDao) {

    /** Sincroniza tudo e devolve avisos (ex.: banco pedindo reconexão). */
    suspend fun sync(): List<String> = withContext(Dispatchers.IO) {
        val api = PluggyApi(settings.clientId, settings.clientSecret)
        api.authenticate()
        val warnings = mutableListOf<String>()
        val allAccounts = mutableListOf<Account>()

        for (itemId in settings.itemList()) {
            var institution = "Banco"
            runCatching { api.item(itemId) }.onSuccess { item ->
                val name = item.optJSONObject("connector")?.str("name").orEmpty()
                if (name.isNotBlank()) institution = name
                val status = item.str("status")
                if (status == "LOGIN_ERROR" || status == "OUTDATED") {
                    warnings += "$institution: reconecte no Meu Pluggy ($status)"
                }
            }

            val accounts = api.accounts(itemId).map { o ->
                val cd = o.optJSONObject("creditData")
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
            dao.upsertAccounts(accounts)
            allAccounts += accounts

            for (acc in accounts) {
                val last = dao.lastDate(acc.id)
                val from = if (last == null) LocalDate.now().minusMonths(12)
                else runCatching { LocalDate.parse(last).minusDays(45) }
                    .getOrDefault(LocalDate.now().minusMonths(12))

                val txs = api.transactions(acc.id, from.toString()).map { o ->
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

                dao.deletePending(acc.id) // pendentes mudam de id ao serem efetivadas
                val res = dao.insertTx(txs)
                val existing = txs.filterIndexed { i, _ -> res[i] == -1L }
                if (existing.isNotEmpty()) dao.updateTx(existing)
            }
        }

        val bank = allAccounts.filter { it.type == "BANK" }.sumOf { it.balance }
        val cards = allAccounts.filter { it.type == "CREDIT" }.sumOf { it.balance }
        dao.putSnapshot(Snapshot(LocalDate.now().toString(), bank, cards))
        settings.widgetBank = bank
        settings.widgetCards = cards
        settings.lastSync = System.currentTimeMillis()
        warnings
    }
}
