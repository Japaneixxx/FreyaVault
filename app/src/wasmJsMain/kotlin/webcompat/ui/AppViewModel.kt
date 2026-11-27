package com.japaneixxx.freya.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.japaneixxx.freya.Alerts
import com.japaneixxx.freya.BalanceWidget
import com.japaneixxx.freya.CalendarSync
import com.japaneixxx.freya.Shortcuts
import com.japaneixxx.freya.Biometric
import com.japaneixxx.freya.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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

class AppViewModel {
    private val app: android.content.Context = com.japaneixxx.freya.web.WebContext.current
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = Settings(app)
    private val db = Db.get(app)
    private val dao = db.dao()

    init {
        Privacy.hidden = settings.hideValues
        Cats.custom = settings.customCategories.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }

    private val rawAccounts = dao.accounts().stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val aliases = MutableStateFlow(settings.accountAliases())

    /** Contas e cartões já com o apelido escolhido pelo usuário no lugar do nome do banco. */
    val accounts = combine(rawAccounts, aliases) { list, al ->
        list.map { a -> al[a.id]?.let { a.copy(name = it) } ?: a }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Nome da conta como o banco informa, ignorando o apelido. */
    fun originalAccountName(id: String): String = rawAccounts.value.firstOrNull { it.id == id }?.name.orEmpty()

    fun hasAccountAlias(id: String): Boolean = aliases.value.containsKey(id)

    /** Renomeia só dentro do Freya (nome vazio ou nulo restaura o original); vale após qualquer sincronização. */
    fun renameAccount(id: String, name: String?) {
        settings.setAccountAlias(id, name)
        aliases.value = settings.accountAliases()
    }
    val budgets = dao.budgets().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val goals = dao.goals().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val bills = dao.bills().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val snapshots = dao.snapshots().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val rules = dao.rules().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val investments = dao.investments().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val netItems = dao.netItems().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val envelopes = dao.envelopes().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val billPayments = dao.billPayments().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val incomes = dao.incomes().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val oneTimeIncomes = dao.oneTimeIncomes().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val netSnapshots = dao.netSnapshots().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val txs = combine(dao.transactions(), dao.rules(), dao.metas()) { list, r, m -> buildItems(list, r, m) }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val dismissed = MutableStateFlow(settings.dismissedKeys())

    /** Cobranças duplicadas ou que subiram de preço, menos as que o usuário dispensou. */
    val chargeAlerts = combine(txs, dismissed) { list, gone ->
        detectChargeAlerts(list).filter { it.key !in gone }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

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

    init {
        // Tudo é local: sem backup, trocar de celular zera tudo. Lembra a cada 30 dias (conta a partir do 1º uso).
        val now = System.currentTimeMillis()
        if (settings.lastBackupAt == 0L) settings.lastBackupAt = now
        else if (settings.configured && now - settings.lastBackupAt > 30L * 24 * 60 * 60 * 1000) {
            message = "Faz mais de 30 dias do último backup. Faça um em Mais > Backup."
        }
    }
    var lastSync by mutableStateOf(settings.lastSync)
    var bankUpdatedAt by mutableStateOf(settings.bankUpdatedAt)

    var locked by mutableStateOf(settings.lockEnabled)
    var lockEnabled by mutableStateOf(settings.lockEnabled)
    var screenCaptureProtection by mutableStateOf(settings.screenCaptureProtection)
        private set
    var pinEnabled by mutableStateOf(settings.hasPin)
    var alertsEnabled by mutableStateOf(settings.alertsEnabled)
    var checkUpdatesOnOpen by mutableStateOf(settings.checkUpdatesOnOpen)
    var calendarEnabled by mutableStateOf(settings.calendarEnabled)
        private set
    var calendarBills by mutableStateOf(settings.calendarBills)
        private set
    var calendarShowAmount by mutableStateOf(settings.calendarShowAmount)
        private set
    var calendarLeadDays by mutableStateOf(settings.calendarLeadDays)
        private set
    var calendarLabel by mutableStateOf(settings.calendarLabel)
    var themeMode by mutableStateOf(settings.themeMode)

    // ---- Aparência, abas e atalhos personalizados ----
    var accent by mutableIntStateOf(settings.accentColor)
        private set
    var fontScalePct by mutableIntStateOf(settings.fontScalePct)
        private set
    var uiDensity by mutableIntStateOf(settings.uiDensity)
        private set
    var startTab by mutableIntStateOf(settings.startTab)
        private set
    var tabOrder by mutableStateOf(parseTabOrder(settings.tabOrder))
    var shortcutIds by mutableStateOf(parseShortcutIds(settings.shortcutIds))
    /** Destino pedido por um atalho ou widget; o App consome depois do desbloqueio. */
    var pendingLink by mutableStateOf<DeepLink?>(null)
    /** Pedido para abrir a tela de novo lançamento (atalho "Novo lançamento"). */
    var addRequest by mutableStateOf(false)

    /** Muda a cor só na tela, enquanto o usuário arrasta os controles; use [updateAccent] para gravar. */
    fun previewAccent(argb: Int) { accent = argb }
    fun updateAccent(argb: Int) {
        settings.accentColor = argb
        accent = argb
        BalanceWidget.refresh(app)
    }
    fun updateFontScalePct(pct: Int) { settings.fontScalePct = pct; fontScalePct = settings.fontScalePct }
    fun updateUiDensity(level: Int) { settings.uiDensity = level; uiDensity = settings.uiDensity }
    fun updateStartTab(tab: Int) { settings.startTab = tab; startTab = settings.startTab }
    fun updateTabOrder(order: List<Int>) { tabOrder = order; settings.tabOrder = encodeTabOrder(order) }
    fun setShortcuts(ids: List<String>) {
        val clean = ids.distinct().take(MAX_SHORTCUTS)
        shortcutIds = clean
        settings.shortcutIds = clean.joinToString(",")
        Shortcuts.publish(app, clean)
    }

    // ---- Início personalizável ----
    var homeLayout by mutableStateOf(parseHomeLayout(settings.homeLayout))
    fun updateHomeLayout(layout: List<HomeBlock>) {
        homeLayout = layout
        settings.homeLayout = encodeHomeLayout(layout)
    }

    // ---- Fatura por ciclo ----
    fun closingDayFor(id: String): Int? = settings.cardClosingDay(id)
    fun bankClosingDayFor(id: String): Int? =
        settings.cardCloseDates()[id]?.let { runCatching { java.time.LocalDate.parse(it).dayOfMonth }.getOrNull() }
    fun setCardClosingDay(id: String, day: Int?) = settings.setCardClosingDay(id, day)

    // ---- Tutorial do primeiro acesso ----
    var tourActive by mutableStateOf(settings.tourPending)
    var tourStep by mutableIntStateOf(0)
    fun startTour() { tourStep = 0; settings.tourPending = true; tourActive = true }
    fun finishTour() { settings.tourPending = false; tourActive = false; tourStep = 0 }
    var updateRelease by mutableStateOf<UpdateRelease?>(null)
    var checkingUpdate by mutableStateOf(false)
    var downloadingUpdate by mutableStateOf(false)

    fun saveCredentials(clientId: String, secret: String, itemIds: String) {
        val firstAccess = !settings.configured
        settings.clientId = clientId
        settings.clientSecret = secret
        settings.itemIds = itemIds
        configured = settings.configured
        if (firstAccess && configured) startTour()
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
        scope.launch {
            connecting = true
            setupError = null
            try {
                withContext(Dispatchers.Default) {
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
        val forced = settings.syncOnReturn
        if (forced) settings.syncOnReturn = false
        if (configured && (forced || System.currentTimeMillis() - settings.lastSync > 15 * 60_000L)) sync()
    }

    /** O usuário foi atualizar os bancos no site do Meu Pluggy: ao voltar, sincroniza sem esperar o intervalo mínimo. */
    fun markOpenedMeuPluggy() {
        settings.syncOnReturn = true
    }

    fun sync() {
        if (syncing || !configured) return
        scope.launch {
            syncing = true
            try {
                val warnings = Repository(settings, db).sync()
                lastSync = settings.lastSync
                bankUpdatedAt = settings.bankUpdatedAt
                val matched = withContext(Dispatchers.Default) { matchBillPayments() }
                message = warnings.takeIf { it.isNotEmpty() }?.take(3)?.joinToString("\n")
                    ?: if (matched > 0) "$matched conta(s) marcada(s) como paga(s) pelo extrato" else null
                withContext(Dispatchers.Default) { settings.pruneSeen() }
                BalanceWidget.refresh(app)
                Alerts.check(app)
                syncCalendar()
            } catch (e: UnknownHostException) {
                message = "Sem conexão com a internet"
            } catch (e: Exception) {
                message = e.message ?: "Falha ao sincronizar"
            } finally {
                syncing = false
            }
        }
    }

    /** A verificação manual (`showNoUpdate = true`) também oferece versões beta; a automática só as estáveis. */
    fun checkForUpdate(showNoUpdate: Boolean = true, includeBeta: Boolean = showNoUpdate) {
        if (checkingUpdate || downloadingUpdate) return
        scope.launch {
            checkingUpdate = true
            try {
                updateRelease = Updater.check(includeBeta)
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
        scope.launch {
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

    fun updateScreenCaptureProtection(enabled: Boolean) {
        settings.screenCaptureProtection = enabled
        screenCaptureProtection = enabled
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

    /** Falso se o PIN estiver errado ou se houver espera por tentativas erradas (a tela de bloqueio mostra o tempo). */
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
        if (enabled) scope.launch { Alerts.check(app) }
    }

    // ---- Agenda do celular ----
    fun updateCalendarEnabled(enabled: Boolean) {
        if (enabled && !CalendarSync.hasPermission(app)) {
            message = "Permita o acesso à agenda para ativar os lembretes"
            return
        }
        settings.calendarEnabled = enabled
        calendarEnabled = enabled
        if (enabled) syncCalendar(announce = true)
        else scope.launch {
            runCatching { withContext(Dispatchers.Default) { CalendarSync.clear(app) } }
                .onFailure { message = "Não foi possível remover os lembretes da agenda" }
        }
    }

    fun updateCalendarBills(on: Boolean) { settings.calendarBills = on; calendarBills = on; syncCalendar() }
    fun updateCalendarShowAmount(on: Boolean) { settings.calendarShowAmount = on; calendarShowAmount = on; syncCalendar() }
    fun updateCalendarLeadDays(days: Int) { settings.calendarLeadDays = days; calendarLeadDays = settings.calendarLeadDays; syncCalendar() }

    /** Idempotente: cria, atualiza e remove os lembretes conforme os vencimentos atuais. */
    fun syncCalendar(announce: Boolean = false) {
        if (!settings.calendarEnabled) return
        scope.launch {
            try {
                val r = withContext(Dispatchers.Default) { CalendarSync.sync(app) }
                calendarLabel = settings.calendarLabel
                if (announce && r != null) {
                    message = when {
                        r.total == 0 -> "Agenda ligada. Nenhuma fatura a vencer nos próximos 60 dias por enquanto."
                        else -> "Agenda ligada: ${r.total} vencimento(s) no calendário ${r.calendar}."
                    }
                }
            } catch (e: SecurityException) {
                settings.calendarEnabled = false
                calendarEnabled = false
                message = "O acesso à agenda foi removido; os lembretes foram desligados"
            } catch (e: Exception) {
                message = "Não foi possível atualizar a agenda do celular: ${e.message ?: "erro desconhecido"}"
            }
        }
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
        scope.launch { dao.putRule(Rule(ruleKeyFor(item.tx), category)) }
    }

    fun setCategoryBulk(items: List<TxItem>, category: String) {
        scope.launch {
            items.map { ruleKeyFor(it.tx) }.distinct().forEach { dao.putRule(Rule(it, category)) }
        }
    }

    fun setIgnoredBulk(items: List<TxItem>, ignored: Boolean) {
        scope.launch { items.forEach { dao.putMeta(TxMeta(it.tx.id, it.note, ignored)) } }
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
        scope.launch { dao.deleteRule(key) }
    }

    fun setNote(item: TxItem, note: String) {
        if (note == item.note) return
        scope.launch { dao.putMeta(TxMeta(item.tx.id, note, item.metaIgnored)) }
    }

    fun setIgnored(item: TxItem, ignored: Boolean) {
        scope.launch { dao.putMeta(TxMeta(item.tx.id, item.note, ignored)) }
    }

    /** [accountId] = id de uma conta de dinheiro (cash:...) ou o padrão "manual" (lançamento sem conta). */
    fun addManual(description: String, amount: Double, date: String, category: String, accountId: String = MANUAL_ACCOUNT_ID) {
        scope.launch {
            val prefix = if (isCashAccountId(accountId)) "cash-" else "manual-"
            db.withTransaction {
                dao.putTx(
                    Tx(
                        id = prefix + UUID.randomUUID(), accountId = accountId, date = date,
                        description = description, amount = amount, pluggyCategory = category, pending = false
                    )
                )
                // O saldo da conta de dinheiro acompanha os lançamentos dela.
                if (isCashAccountId(accountId)) dao.addToBalance(accountId, amount)
            }
            if (isCashAccountId(accountId)) refreshTotals()
        }
    }

    fun deleteManual(tx: Tx) {
        if (!isManualAccountId(tx.accountId)) return
        scope.launch {
            db.withTransaction {
                dao.deleteTx(tx.id)
                if (isCashAccountId(tx.accountId)) dao.addToBalance(tx.accountId, -tx.amount)
            }
            if (isCashAccountId(tx.accountId)) refreshTotals()
        }
    }

    // ----- dinheiro físico (conta criada à mão) -----
    val cashAccounts get() = accounts.value.filter { isCashAccountId(it.id) }

    fun addCashAccount(name: String, initialBalance: Double) {
        val n = name.trim().ifBlank { "Carteira" }
        scope.launch {
            db.withTransaction {
                dao.putAccount(
                    Account(
                        id = CASH_PREFIX + UUID.randomUUID(), itemId = MANUAL_ITEM_ID, institution = "Dinheiro",
                        name = n, type = "BANK", subtype = "CASH", balance = initialBalance.r2(), currency = "BRL"
                    )
                )
            }
            refreshTotals()
        }
    }

    /** Acerta o saldo com o que o usuário realmente tem na mão, registrando a diferença como ajuste (fora dos totais). */
    fun adjustCashBalance(accountId: String, realBalance: Double) {
        val current = rawAccounts.value.firstOrNull { it.id == accountId } ?: return
        val delta = (realBalance - current.balance).r2()
        if (delta == 0.0) return
        scope.launch {
            db.withTransaction {
                dao.putTx(
                    Tx(
                        id = "cash-adj-" + UUID.randomUUID(), accountId = accountId, date = java.time.LocalDate.now().toString(),
                        description = "Ajuste de saldo", amount = delta, pluggyCategory = CAT_OWN_TRANSFER, pending = false
                    )
                )
                dao.setBalance(accountId, realBalance.r2())
            }
            refreshTotals()
        }
    }

    fun deleteCashAccount(accountId: String) {
        if (!isCashAccountId(accountId)) return
        scope.launch {
            db.withTransaction {
                dao.deleteTxOfAccount(accountId)
                dao.deleteAccount(accountId)
            }
            renameAccount(accountId, null)
            refreshTotals()
        }
    }

    /** Recalcula patrimônio, foto do dia e widget depois de mudar saldos fora do sync. */
    private suspend fun refreshTotals() {
        withContext(Dispatchers.Default) {
            val all = dao.accountsNow()
            val bank = bankTotal(all); val cards = cardsTotal(all)
            settings.widgetBank = bank
            settings.widgetCards = cards
            dao.putSnapshot(Snapshot(java.time.LocalDate.now().toString(), bank, cards))
            dao.recordNetSnapshot()
        }
        BalanceWidget.refresh(app)
    }

    // ----- orçamentos -----
    /** [envelope] = true acumula a sobra de cada mês para o seguinte (o início conta a partir do mês atual). */
    fun saveBudget(category: String, limit: Double, envelope: Boolean = false) {
        scope.launch {
            dao.putBudget(Budget(category, limit))
            val already = envelopes.value.any { it.category == category }
            if (envelope && !already) dao.putEnvelope(Envelope(category, monthKey()))
            if (!envelope && already) dao.deleteEnvelope(category)
        }
    }

    fun deleteBudget(category: String) {
        scope.launch {
            dao.deleteBudget(category)
            dao.deleteEnvelope(category)
        }
    }

    // ----- patrimônio -----
    fun saveNetItem(item: NetItem) {
        scope.launch { dao.putNetItem(item); dao.recordNetSnapshot() }
    }

    fun deleteNetItem(item: NetItem) {
        scope.launch { dao.deleteNetItem(item.id); dao.recordNetSnapshot() }
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
        scope.launch {
            totals.filterKeys { it in Cats.budget }.entries
                .sortedByDescending { it.value }.take(6)
                .forEach { (c, v) -> dao.putBudget(Budget(c, Math.ceil(v / 3 / 10.0) * 10.0)) }
        }
    }

    // ----- metas -----
    fun addGoal(name: String, target: Double) {
        scope.launch { dao.putGoal(Goal(name = name, target = target)) }
    }

    fun adjustGoal(goal: Goal, delta: Double) {
        scope.launch { dao.putGoal(goal.copy(saved = (goal.saved + delta).coerceAtLeast(0.0))) }
    }

    fun deleteGoal(goal: Goal) {
        scope.launch { dao.deleteGoal(goal.id) }
    }

    // ----- contas a pagar -----
    fun saveBill(bill: Bill) {
        scope.launch { dao.putBill(bill) }
    }

    fun deleteBill(bill: Bill) {
        scope.launch {
            dao.deleteBill(bill.id)
            dao.deletePaymentsOf(bill.id)
        }
    }

    /** Marca como paga no mês. [actual] é o valor real (contas variáveis); sem ele, usa o valor esperado. */
    fun setBillPaid(bill: Bill, paid: Boolean, actual: Double? = null) {
        scope.launch {
            val month = monthKey()
            dao.putBill(bill.copy(lastPaidMonth = if (paid) month else ""))
            if (paid) dao.putBillPayment(BillPayment(bill.id, month, actual ?: billEstimate(bill, billPayments.value)))
            else dao.deleteBillPayment(bill.id, month)
        }
    }

    fun saveIncome(income: Income) {
        scope.launch { dao.putIncome(income) }
    }

    fun deleteIncome(income: Income) {
        scope.launch { dao.deleteIncome(income.id) }
    }

    fun saveOneTimeIncomes(incomes: List<OneTimeIncome>) {
        scope.launch {
            db.withTransaction {
                incomes.forEach { dao.putOneTimeIncome(it) }
            }
        }
    }

    fun deleteOneTimeIncome(income: OneTimeIncome) {
        scope.launch { dao.deleteOneTimeIncome(income.id) }
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
        scope.launch {
            val n = withContext(Dispatchers.Default) { matchBillPayments() }
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
                .append(formatNumber(Locale.US, "%.2f", it.tx.amount)).append(';')
                .append(clean(a)).append(';').append(clean(it.note)).append('\n')
        }
        return sb.toString()
    }

    /** Gera o backup; com [password] o arquivo sai criptografado (AES-256-GCM). */
    suspend fun backupJson(password: CharArray? = null): String = withContext(Dispatchers.Default) {
        val plain = Backup.export(dao, settings.accountAliases(), Cats.custom)
        if (password != null && password.isNotEmpty()) BackupCrypto.encrypt(plain, password) else plain
    }

    fun markBackupDone() { settings.lastBackupAt = System.currentTimeMillis() }

    fun isBackupEncrypted(text: String): Boolean = BackupCrypto.isEncrypted(text)

    /** [replace] troca os dados do backup pelos dele; false mescla sem sobrescrever nem duplicar. */
    fun restoreBackup(json: String, replace: Boolean = false, password: CharArray? = null) {
        scope.launch {
            message = try {
                val n = withContext(Dispatchers.Default) {
                    val plain = if (BackupCrypto.isEncrypted(json)) {
                        if (password == null) throw BadBackupPasswordException()
                        BackupCrypto.decrypt(json, password)
                    } else json
                    Backup.import(dao, { block -> db.withTransaction { block() } }, plain, replace,
                        onAccountAliases = { restored ->
                            settings.mergeAccountAliases(restored)
                            aliases.value = settings.accountAliases()
                        },
                        onCustomCategories = { list ->
                            val merged = (Cats.custom + list).distinct()
                            settings.customCategories = merged.joinToString("|")
                            Cats.custom = merged
                        })
                }
                refreshTotals()
                "Backup restaurado ($n itens)"
            } catch (e: BadBackupPasswordException) {
                e.message
            } catch (e: IllegalArgumentException) {
                e.message ?: "Backup incompatível"
            } catch (e: org.json.JSONException) {
                "Arquivo de backup inválido"
            } catch (e: Exception) {
                "Não foi possível restaurar o backup (${e.message ?: "erro"}). Nada foi alterado."
            }
        }
    }

    fun monthSummary(): String = monthSummaryText(txs.value.filter { !it.ignored }, monthKey())

    fun wipeLocalData() {
        scope.launch(Dispatchers.Default) {
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
            settings.bankUpdatedAt = 0L
            bankUpdatedAt = 0L
        }
    }
}
