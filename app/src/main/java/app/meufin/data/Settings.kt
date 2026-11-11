package app.meufin.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    var alertsEnabled: Boolean get() = bool("alerts"); set(v) = put("alerts", v)

    /** 0 = sistema, 1 = claro, 2 = escuro */
    var themeMode: Int
        get() = prefs.getInt("theme", 0)
        set(v) { prefs.edit().putInt("theme", v).apply() }

    /** Categorias criadas pelo usuário, separadas por "|". */
    var customCategories: String get() = str("custom_cats"); set(v) = put("custom_cats", v)

    // usados pelo widget
    var widgetBank: Double
        get() = str("w_bank").toDoubleOrNull() ?: 0.0
        set(v) = put("w_bank", v.toString())
    var widgetCards: Double
        get() = str("w_cards").toDoubleOrNull() ?: 0.0
        set(v) = put("w_cards", v.toString())

    // evita notificar a mesma coisa duas vezes
    fun seen(key: String): Boolean = bool("seen_$key")
    fun markSeen(key: String) = put("seen_$key", true)

    val configured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank() && itemList().isNotEmpty()

    fun itemList(): List<String> =
        itemIds.split(Regex("[,;\\s]+")).map { it.trim() }.filter { it.isNotBlank() }.distinct()
}
