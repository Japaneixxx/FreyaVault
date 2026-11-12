package com.japaneixxx.freya.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/** Credenciais e preferências ficam criptografadas (Android Keystore) no próprio aparelho. */
class Settings(ctx: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        ctx,
        "secure_settings",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        put("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        put("pin_hash", hashPin(pin, salt))
    }

    fun clearPin() {
        prefs.edit().remove("pin_hash").remove("pin_salt").apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = runCatching { Base64.decode(str("pin_salt"), Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = str("pin_hash")
        if (expected.isBlank()) return false
        return MessageDigest.isEqual(hashPin(pin, salt).toByteArray(), expected.toByteArray())
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return Base64.encodeToString(digest.digest(salt + pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
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

    val configured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank() && itemList().isNotEmpty()

    fun itemList(): List<String> =
        itemIds.split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotBlank() }.distinct()
}
