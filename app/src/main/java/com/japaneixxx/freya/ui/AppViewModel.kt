package com.japaneixxx.freya.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.japaneixxx.freya.Alerts
import com.japaneixxx.freya.BalanceWidget
import com.japaneixxx.freya.Biometric
import com.japaneixxx.freya.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.util.Locale
import java.util.UUID
import android.net.Uri
import com.japaneixxx.freya.BuildConfig
import java.time.YearMonth
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow

data class TxItem(
    val tx: Tx,
    val category: String,
    val note: String = "",
    val metaIgnored: Boolean = false
) {
    val ignored: Boolean get() = metaIgnored || category in IGNORED_CATEGORIES
    val manual: Boolean get() = tx.accountId == "manual"

    /** Nome sem texto redundante do banco + tipo (Pix, débito, fatura...) como sinalização. */
    val clean: Clean by lazy(LazyThreadSafetyMode.NONE) { sanitize(tx.description, tx.amount) }
}

class AppViewModel(private val app: Application) : AndroidViewModel(app) {
    val settings = Settings(app)
    private val dao = Db.get(app).dao()

    init {
        Privacy.hidden = settings.hideValues
        Cats.custom = settings.customCategories.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }

    private val rawAccounts = dao.accounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val aliases = MutableStateFlow(settings.accountAliases())

    /** Contas e cartões já com o apelido escolhido pelo usuário no lugar do nome do banco. */
    val accounts = combine(rawAccounts, aliases) { list, al ->
        list.map { a -> al[a.id]?.let { a.copy(name = it) } ?: a }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Nome da conta como o banco informa, ignorando o apelido. */
    fun originalAccountName(id: String): String = rawAccounts.value.firstOrNull { it.id == id }?.name.orEmpty()

    fun hasAccountAlias(id: String): Boolean = aliases.value.containsKey(id)

    /** Renomeia só dentro do Freya (nome vazio ou nulo restaura o original); vale após qualquer sincronização. */
    fun renameAccount(id: String, name: String?) {
        settings.setAccountAlias(id, name)
        aliases.value = settings.accountAliases()
    }
    val budgets = dao.budgets().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val goals = dao.goals().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bills = dao.bills().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val snapshots = dao.snapshots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val rules = dao.rules().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val investments = dao.investments().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val netItems = dao.netItems().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val envelopes = dao.envelopes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val billPayments = dao.billPayments().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val incomes = dao.incomes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val oneTimeIncomes = dao.oneTimeIncomes().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val netSnapshots = dao.netSnapshots().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val txs = combine(dao.transactions(), dao.rules(), dao.metas()) { list, r, m -> buildItems(list, r, m) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val dismissed = MutableStateFlow(settings.dismissedKeys())

    /** Cobranças duplicadas ou que subiram de preço, menos as que o usuário dispensou. */
    val chargeAlerts = combine(txs, dismissed) { list, gone ->
        detectChargeAlerts(list).filter { it.key !in gone }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Versão anterior vista pelo usuário (vazia = desconhecida). Não nulo => mostrar "o que há de novo". */
    var whatsNewFrom by mutableStateOf<String?>(null)
    val currentVersion: String = BuildConfig.VERSION_NAME

    init {
        val seen = settings.lastSeenVersion
        when {
            seen == currentVersion -> Unit
            seen.isBlank() && !settings.configured -> settings.lastSeenVersion = currentVersion // instalação nova
            // versão sem entrada no histórico (ex.: só correção): não incomoda o usuário
            notesSince(seen, currentVersion).isEmpty() -> settings.lastSeenVersion = currentVersion
            else -> whatsNewFrom = seen
        }
    }

    fun dismissWhatsNew() {
        settings.lastSeenVersion = currentVersion
        whatsNewFrom = null
    }

    var configured by mutableStateOf(settings.configured)
    var connecting by mutableStateOf(false)
    var setupError by mutableStateOf<String?>(null)
    var syncing by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var lastSync by mutableStateOf(settings.lastSync)

    var locked by mutableStateOf(settings.lockEnabled)
    var lockEnabled by mutableStateOf(settings.lockEnabled)
    var pinEnabled by mutableStateOf(settings.hasPin)
    var alertsEnabled by mutableStateOf(settings.alertsEnabled)
    var checkUpdatesOnOpen by mutableStateOf(settings.checkUpdatesOnOpen)
    var themeMode by mutableStateOf(settings.themeMode)
    var updateRelease by mutableStateOf<UpdateRelease?>(null)
    var checkingUpdate by mutableStateOf(false)
    var downloadingUpdate by mutableStateOf(false)

    fun saveCredentials(clientId: String, secret: String, itemIds: String) {
        settings.clientId = clientId
        settings.clientSecret = secret
        settings.itemIds = itemIds
        configured = settings.configured
        sync()
    }

    /**
     * Valida as credenciais no Pluggy (autenticação + cada Item ID) antes de salvar, com mensagens claras.
     * Só grava e sincroniza se tudo estiver certo.
     */
    fun testAndSaveCredentials(clientId: String, secret: String, itemIds: String) {
        if (connecting) return
        val ids = itemIds.split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        ids.firstOrNull { !uuid.matches(it) }?.let {
            setupError = "O Item ID \"$it\" não parece válido. Ele tem o formato 8-4-4-4-12 caracteres (ex.: 207f5bcd-312a-439c-abbe-166b6632c980)."
            return
        }
        if (ids.size > 5) {
            setupError = "O Meu Pluggy permite até 5 conexões."
            return
        }
        viewModelScope.launch {
            connecting = true
            setupError = null
            try {
                withContext(Dispatchers.IO) {
                    val api = PluggyApi(clientId, secret)
                    api.authenticate()
                    ids.forEach { api.item(it) }
                }
                saveCredentials(clientId, secret, ids.joinToString("\n"))
            } catch (e: UnknownHostException) {
                setupError = "Sem conexão com a internet."
            } catch (e: Exception) {
                setupError = e.message ?: "Não foi possível conectar ao Pluggy."
            } finally {
                connecting = false
            }
        }
    }

    /** Chamado em onStart: sincroniza ao abrir o app, no máximo a cada 15 min. */
    fun syncIfStale() {
        if (configured && System.currentTimeMillis() - settings.lastSync > 15 * 60_000L) sync()
    }

    fun sync() {
        if (syncing || !configured) return
        viewModelScope.launch {
            syncing = true
            try {
                val warnings = Repository(settings, dao).sync()
                lastSync = settings.lastSync
                val matched = withContext(Dispatchers.IO) { matchBillPayments() }
                message = warnings.firstOrNull()
                    ?: if (matched > 0) "$matched conta(s) marcada(s) como paga(s) pelo extrato" else null
                BalanceWidget.refresh(app)
                Alerts.check(app)
            } catch (e: UnknownHostException) {
                message = "Sem conexão com a internet"
            } catch (e: Exception) {
                message = e.message ?: "Falha ao sincronizar"
            } finally {
                syncing = false
            }
        }
    }

    fun checkForUpdate(showNoUpdate: Boolean = true) {
        if (!configured || checkingUpdate || downloadingUpdate) return
        viewModelScope.launch {
            checkingUpdate = true
            try {
                updateRelease = Updater.check()
                if (showNoUpdate && updateRelease == null) {
                    message = "Você já está usando a versão mais recente"
                }
            } catch (e: Exception) {
                if (showNoUpdate) message = e.message ?: "Não foi possível verificar atualizações"
            } finally {
                checkingUpdate = false
            }
        }
    }

    fun downloadUpdate(onReady: (Uri) -> Unit) {
        val release = updateRelease ?: return
        if (downloadingUpdate) return
        viewModelScope.launch {
            downloadingUpdate = true
            try {
                onReady(Updater.download(app, release))
            } catch (e: Exception) {
                message = e.message ?: "Não foi possível baixar a atualização"
            } finally {
                downloadingUpdate = false
            }
        }
    }

    // ----- preferências -----
    fun toggleHide() {
        Privacy.hidden = !Privacy.hidden
        settings.hideValues = Privacy.hidden
        BalanceWidget.refresh(app)
    }

    fun setLock(enabled: Boolean) {
        settings.lockEnabled = enabled
        lockEnabled = enabled
        BalanceWidget.refresh(app)
    }

    fun setPin(pin: String) {
        settings.setPin(pin)
        pinEnabled = true
    }

    fun clearPin() {
        settings.clearPin()
        pinEnabled = false
        if (!Biometric.available(app) && lockEnabled) setLock(false)
    }

    fun unlockWithPin(pin: String): Boolean {
        if (!settings.verifyPin(pin)) return false
        locked = false
        return true
    }

    fun setAlerts(enabled: Boolean) {
        if (enabled && !Alerts.notificationsEnabled(app)) {
            settings.alertsEnabled = false
            alertsEnabled = false
            message = "Permita as notificações para ativar os alertas"
            return
        }
        settings.alertsEnabled = enabled
        alertsEnabled = enabled
        Alerts.schedule(app, enabled)
        if (enabled) viewModelScope.launch { Alerts.check(app) }
    }

    fun updateCheckUpdatesOnOpen(enabled: Boolean) {
        settings.checkUpdatesOnOpen = enabled
        checkUpdatesOnOpen = enabled
    }

    fun setTheme(mode: Int) {
        settings.themeMode = mode
        themeMode = mode
    }

    // ----- transações -----
    fun setCategory(item: TxItem, category: String) {
        viewModelScope.launch { dao.putRule(Rule(ruleKeyFor(item.tx), category)) }
    }

    fun setCategoryBulk(items: List<TxItem>, category: String) {
        viewModelScope.launch {
            items.map { ruleKeyFor(it.tx) }.distinct().forEach { dao.putRule(Rule(it, category)) }
        }
    }

    fun setIgnoredBulk(items: List<TxItem>, ignored: Boolean) {
        viewModelScope.launch { items.forEach { dao.putMeta(TxMeta(it.tx.id, it.note, ignored)) } }
    }

    fun addCategory(name: String) {
        val n = name.trim()
        if (n.isBlank() || n in Cats.all) return
        Cats.custom = Cats.custom + n
        settings.customCategories = Cats.custom.joinToString("|")
    }

    fun removeCategory(name: String) {
        Cats.custom = Cats.custom - name
        settings.customCategories = Cats.custom.joinToString("|")
    }

    fun deleteRule(key: String) {
        viewModelScope.launch { dao.deleteRule(key) }
    }

    fun setNote(item: TxItem, note: String) {
        if (note == item.note) return
        viewModelScope.launch { dao.putMeta(TxMeta(item.tx.id, note, item.metaIgnored)) }
    }

    fun setIgnored(item: TxItem, ignored: Boolean) {
        viewModelScope.launch { dao.putMeta(TxMeta(item.tx.id, item.note, ignored)) }
    }

    fun addManual(description: String, amount: Double, date: String, category: String) {
        viewModelScope.launch {
            dao.putTx(
                Tx(
                    id = "manual-" + UUID.randomUUID(), accountId = "manual", date = date,
                    description = description, amount = amount, pluggyCategory = category, pending = false
                )
            )
        }
    }

    fun deleteManual(tx: Tx) {
        if (tx.accountId == "manual") viewModelScope.launch { dao.deleteTx(tx.id) }
    }

    // ----- orçamentos -----
    /** [envelope] = true acumula a sobra de cada mês para o seguinte (o início conta a partir do mês atual). */
    fun saveBudget(category: String, limit: Double, envelope: Boolean = false) {
        viewModelScope.launch {
            dao.putBudget(Budget(category, limit))
            val already = envelopes.value.any { it.category == category }
            if (envelope && !already) dao.putEnvelope(Envelope(category, monthKey()))
            if (!envelope && already) dao.deleteEnvelope(category)
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            dao.deleteBudget(category)
            dao.deleteEnvelope(category)
        }
    }

    // ----- patrimônio -----
    fun saveNetItem(item: NetItem) {
        viewModelScope.launch { dao.putNetItem(item); dao.recordNetSnapshot() }
    }

    fun deleteNetItem(item: NetItem) {
        viewModelScope.launch { dao.deleteNetItem(item.id); dao.recordNetSnapshot() }
    }

    // ----- cobranças suspeitas -----
    fun dismissChargeAlert(key: String) {
        settings.markSeen("dismiss_$key")
        dismissed.value = dismissed.value + key
    }

    /** Sugere orçamento = média dos últimos 3 meses fechados nas 6 maiores categorias. */
    fun suggestBudgets() {
        val real = txs.value.filter { !it.ignored }
        val totals = HashMap<String, Double>()
        (1..3).map { monthKey(it) }.forEach { m ->
            spendByCategory(real.inMonth(m)).forEach { (c, v) -> totals[c] = (totals[c] ?: 0.0) + v }
        }
        viewModelScope.launch {
            totals.filterKeys { it in Cats.budget }.entries
                .sortedByDescending { it.value }.take(6)
                .forEach { (c, v) -> dao.putBudget(Budget(c, Math.ceil(v / 3 / 10.0) * 10.0)) }
        }
    }

    // ----- metas -----
    fun addGoal(name: String, target: Double) {
        viewModelScope.launch { dao.putGoal(Goal(name = name, target = target)) }
    }

    fun adjustGoal(goal: Goal, delta: Double) {
        viewModelScope.launch { dao.putGoal(goal.copy(saved = (goal.saved + delta).coerceAtLeast(0.0))) }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { dao.deleteGoal(goal.id) }
    }

    // ----- contas a pagar -----
    fun saveBill(bill: Bill) {
        viewModelScope.launch { dao.putBill(bill) }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            dao.deleteBill(bill.id)
            dao.deletePaymentsOf(bill.id)
        }
    }

    /** Marca como paga no mês. [actual] é o valor real (contas variáveis); sem ele, usa o valor esperado. */
    fun setBillPaid(bill: Bill, paid: Boolean, actual: Double? = null) {
        viewModelScope.launch {
            val month = monthKey()
            dao.putBill(bill.copy(lastPaidMonth = if (paid) month else ""))
            if (paid) dao.putBillPayment(BillPayment(bill.id, month, actual ?: billEstimate(bill, billPayments.value)))
            else dao.deleteBillPayment(bill.id, month)
        }
    }

    fun saveIncome(income: Income) {
        viewModelScope.launch { dao.putIncome(income) }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch { dao.deleteIncome(income.id) }
    }

    fun saveOneTimeIncome(income: OneTimeIncome) {
        viewModelScope.launch { dao.putOneTimeIncome(income) }
    }

    fun deleteOneTimeIncome(income: OneTimeIncome) {
        viewModelScope.launch { dao.deleteOneTimeIncome(income.id) }
    }

    /**
     * Dá baixa em compromissos cuja palavra-chave apareceu no extrato do mês.
     * Contas fixas/financiamentos só casam se o valor estiver até 25% do esperado.
     */
    private suspend fun matchBillPayments(): Int {
        val ym = YearMonth.now()
        val month = ym.toString()
        val candidates = dao.billsNow().filter {
            it.lastPaidMonth != month && !it.matchText.isNullOrBlank() && isBillActive(it, ym)
        }
        if (candidates.isEmpty()) return 0
        val expenses = buildItems(dao.txNow(), dao.rulesNow(), dao.metasNow())
            .filter { it.tx.amount < 0 && !it.manual && !it.tx.pending && it.tx.date.startsWith(month) }
        var count = 0
        candidates.forEach { b ->
            val key = foldText(b.matchText.orEmpty().trim())
            val hit = expenses.filter {
                foldText(it.tx.description).contains(key) &&
                    (b.kind == KIND_VARIABLE || abs(-it.tx.amount - b.amount) <= b.amount * 0.25)
            }.maxByOrNull { it.tx.date }
            if (hit != null) {
                dao.putBill(b.copy(lastPaidMonth = month))
                dao.putBillPayment(BillPayment(b.id, month, -hit.tx.amount))
                count++
            }
        }
        return count
    }

    fun checkBillPayments() {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { matchBillPayments() }
            message = if (n > 0) "$n conta(s) marcada(s) como paga(s) pelo extrato"
            else "Nenhum pagamento novo encontrado no extrato"
        }
    }

    // ----- exportação / backup / limpeza -----
    fun buildCsv(): String {
        val sb = StringBuilder("data;descricao;categoria;valor;conta;observacao\n")
        val acc = accounts.value.associateBy { it.id }
        fun clean(s: String) = s.replace(";", ",").replace("\n", " ")
        txs.value.forEach {
            val a = acc[it.tx.accountId]?.let { x -> "${x.institution} ${x.name}" } ?: "Manual"
            sb.append(it.tx.date).append(';').append(clean(it.tx.description)).append(';')
                .append(clean(it.category)).append(';')
                .append(String.format(Locale.US, "%.2f", it.tx.amount)).append(';')
                .append(clean(a)).append(';').append(clean(it.note)).append('\n')
        }
        return sb.toString()
    }

    suspend fun backupJson(): String = withContext(Dispatchers.IO) { Backup.export(dao, settings.accountAliases()) }

    fun restoreBackup(json: String) {
        viewModelScope.launch {
            message = try {
                val n = withContext(Dispatchers.IO) {
                    Backup.import(dao, json) { restored ->
                        settings.mergeAccountAliases(restored)
                        aliases.value = settings.accountAliases()
                    }
                }
                "Backup restaurado ($n itens)"
            } catch (e: Exception) {
                "Arquivo de backup inválido"
            }
        }
    }

    fun monthSummary(): String = monthSummaryText(txs.value.filter { !it.ignored }, monthKey())

    fun wipeLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearTx(); dao.clearAccounts(); dao.clearRules()
            dao.clearBudgets(); dao.clearGoals(); dao.clearMetas()
            dao.clearBills(); dao.clearSnapshots()
            dao.clearBillPayments(); dao.clearIncomes()
            dao.clearOneTimeIncomes()
            dao.clearInvestments(); dao.clearNetItems(); dao.clearEnvelopes(); dao.clearNetSnapshots()
            settings.clearAccountAliases()
            aliases.value = emptyMap()
            settings.lastSync = 0L
            lastSync = 0L
        }
    }
}
