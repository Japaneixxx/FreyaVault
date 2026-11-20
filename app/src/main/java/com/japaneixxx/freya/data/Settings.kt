package com.japaneixxx.freya.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.LocalDate
import java.util.Base64

/** Credenciais e preferências ficam criptografadas (Android Keystore) no próprio aparelho. */
class Settings(ctx: Context) {
    private val prefs: SharedPreferences = openPrefs(ctx)

    private companion object {
        fun create(ctx: Context): SharedPreferences = EncryptedSharedPreferences.create(
            ctx,
            "secure_settings",
            MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        /**
         * Em alguns aparelhos o Keystore corrompe (restauração de backup do sistema, troca de bloqueio de tela) e o
         * arquivo criptografado fica ilegível: o app travaria já na abertura. Nesse caso recomeça com preferências
         * limpas (será preciso informar o Pluggy de novo; os dados do banco local continuam).
         */
        fun openPrefs(ctx: Context): SharedPreferences = try {
            create(ctx)
        } catch (e: Exception) {
            ctx.deleteSharedPreferences("secure_settings")
            create(ctx)
        }
    }

    private fun str(key: String) = prefs.getString(key, "") ?: ""
    private fun put(key: String, v: String) = prefs.edit().putString(key, v).apply()
    private fun bool(key: String) = prefs.getBoolean(key, false)
    private fun put(key: String, v: Boolean) = prefs.edit().putBoolean(key, v).apply()

    var clientId: String get() = str("cid"); set(v) = put("cid", v)
    var clientSecret: String get() = str("secret"); set(v) = put("secret", v)
    var itemIds: String get() = str("items"); set(v) = put("items", v)

    var lastSync: Long
        get() = prefs.getLong("last_sync", 0L)
        set(v) { prefs.edit().putLong("last_sync", v).apply() }

    /** Quando o Meu Pluggy leu os bancos pela última vez (o mais antigo entre os itens), segundo o campo lastUpdatedAt. */
    var bankUpdatedAt: Long
        get() = prefs.getLong("bank_updated_at", 0L)
        set(v) { prefs.edit().putLong("bank_updated_at", v).apply() }

    /** Marcado ao abrir o site do Meu Pluggy: o próximo retorno ao app sincroniza sem esperar o intervalo mínimo. */
    var syncOnReturn: Boolean get() = bool("sync_on_return"); set(v) = put("sync_on_return", v)

    var hideValues: Boolean get() = bool("hide"); set(v) = put("hide", v)
    var lockEnabled: Boolean get() = bool("lock"); set(v) = put("lock", v)
    val hasPin: Boolean get() = str("pin_hash").isNotBlank() && str("pin_salt").isNotBlank()
    var alertsEnabled: Boolean get() = bool("alerts"); set(v) = put("alerts", v)
    var alertPromptShown: Boolean
        get() = bool("alert_prompt_shown")
        set(v) = put("alert_prompt_shown", v)
    var checkUpdatesOnOpen: Boolean
        get() = prefs.getBoolean("check_updates_on_open", true)
        set(v) = put("check_updates_on_open", v)

    /** 0 = sistema, 1 = claro, 2 = escuro */
    var themeMode: Int
        get() = prefs.getInt("theme", 0)
        set(v) { prefs.edit().putInt("theme", v).apply() }

    /** Última versão cujo "o que há de novo" o usuário já viu. */
    var lastSeenVersion: String get() = str("last_seen_version"); set(v) = put("last_seen_version", v)

    // Taxas de mercado da calculadora de investimentos (última consulta ao Banco Central ou valor digitado).
    var calcCdi: String get() = str("calc_cdi"); set(v) = put("calc_cdi", v)
    var calcIpca: String get() = str("calc_ipca"); set(v) = put("calc_ipca", v)
    var calcRatesDate: String get() = str("calc_rates_date"); set(v) = put("calc_rates_date", v)
    var calcRatesFetchedAt: Long
        get() = prefs.getLong("calc_rates_fetched_at", 0L)
        set(v) { prefs.edit().putLong("calc_rates_fetched_at", v).apply() }

    /** Categorias criadas pelo usuário, separadas por "|". */
    var customCategories: String get() = str("custom_cats"); set(v) = put("custom_cats", v)

    /** Apelidos de contas e cartões escolhidos pelo usuário (id da conta -> nome). Sobrevivem às sincronizações. */
    fun accountAliases(): Map<String, String> {
        val raw = str("account_aliases")
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val o = org.json.JSONObject(raw)
            o.keys().asSequence().associateWith { o.getString(it) }
        }.getOrDefault(emptyMap())
    }

    private fun saveAccountAliases(map: Map<String, String>) =
        put("account_aliases", if (map.isEmpty()) "" else org.json.JSONObject(map).toString())

    /** Define o apelido de uma conta; nome vazio ou nulo volta ao nome original do banco. */
    fun setAccountAlias(id: String, name: String?) {
        val m = accountAliases().toMutableMap()
        if (name.isNullOrBlank()) m.remove(id) else m[id] = name.trim()
        saveAccountAliases(m)
    }

    /** Mescla apelidos (restauração de backup) por cima dos atuais. */
    fun mergeAccountAliases(extra: Map<String, String>) {
        if (extra.isEmpty()) return
        saveAccountAliases(accountAliases() + extra.filterValues { it.isNotBlank() })
    }

    fun clearAccountAliases() = saveAccountAliases(emptyMap())

    fun setPin(pin: String) {
        val salt = PinHasher.newSalt()
        prefs.edit()
            .putString("pin_salt", Base64.getEncoder().encodeToString(salt))
            .putString("pin_hash", PinHasher.pbkdf2(pin, salt))
            .putString("pin_v", "2")
            .putInt("pin_fails", 0)
            .putLong("pin_lock_until", 0L)
            .apply()
    }

    fun clearPin() {
        prefs.edit().remove("pin_hash").remove("pin_salt").remove("pin_v")
            .remove("pin_fails").remove("pin_lock_until").apply()
    }

    /** Milissegundos que ainda faltam até poder tentar o PIN de novo (0 = liberado). */
    fun pinLockRemainingMs(): Long =
        (prefs.getLong("pin_lock_until", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    fun verifyPin(pin: String): Boolean {
        if (pinLockRemainingMs() > 0) return false
        val salt = runCatching { Base64.getDecoder().decode(str("pin_salt")) }.getOrNull() ?: return false
        val expected = str("pin_hash")
        if (expected.isBlank()) return false
        val v2 = str("pin_v") == "2"
        val ok = PinHasher.same(
            if (v2) PinHasher.pbkdf2(pin, salt) else PinHasher.legacySha256(pin, salt),
            expected
        )
        if (ok) {
            if (!v2) setPin(pin) // migra o PIN antigo (SHA-256) para PBKDF2
            prefs.edit().putInt("pin_fails", 0).putLong("pin_lock_until", 0L).apply()
        } else {
            val fails = prefs.getInt("pin_fails", 0) + 1
            val until = PinHasher.lockDelayMs(fails).let { if (it > 0) System.currentTimeMillis() + it else 0L }
            prefs.edit().putInt("pin_fails", fails).putLong("pin_lock_until", until).apply()
        }
        return ok
    }

    // usados pelo widget
    var widgetBank: Double
        get() = str("w_bank").toDoubleOrNull() ?: 0.0
        set(v) = put("w_bank", v.toString())
    var widgetCards: Double
        get() = str("w_cards").toDoubleOrNull() ?: 0.0
        set(v) = put("w_cards", v.toString())

    // evita notificar a mesma coisa duas vezes
    /** Alertas de cobrança que o usuário marcou como "não é um problema". */
    fun dismissedKeys(): Set<String> =
        prefs.all.keys.filter { it.startsWith("seen_dismiss_") }.map { it.removePrefix("seen_dismiss_") }.toSet()

    fun seen(key: String): Boolean = bool("seen_$key")
    fun markSeen(key: String) = put("seen_$key", true)

    /** Último backup exportado (0 = nunca); base do lembrete mensal. */
    var lastBackupAt: Long
        get() = prefs.getLong("last_backup_at", 0L)
        set(v) { prefs.edit().putLong("last_backup_at", v).apply() }

    /**
     * Cada alerta marcado como visto vira uma chave para sempre. Apaga as que carregam uma data com mais de
     * [keepDays] dias (já não podem repetir). Escolhas do usuário ("dismiss_") e alertas de cobrança ficam.
     */
    fun pruneSeen(today: LocalDate = LocalDate.now(), keepDays: Long = 120) {
        val cutoff = today.minusDays(keepDays)
        val date = Regex("""(\d{4})-(\d{2})(?:-(\d{2}))?""")
        val old = prefs.all.keys.filter { k ->
            k.startsWith("seen_") && !k.startsWith("seen_dismiss_") && !k.startsWith("seen_chg_") &&
                date.find(k)?.let { m ->
                    runCatching {
                        LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toIntOrNull() ?: 1)
                    }.getOrNull()?.isBefore(cutoff) == true
                } == true
        }
        if (old.isNotEmpty()) prefs.edit().apply { old.forEach { remove(it) } }.apply()
    }

    val configured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank() && itemList().isNotEmpty()

    fun itemList(): List<String> =
        itemIds.split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotBlank() }.distinct()
}
