package androidx.security.crypto

import android.content.Context
import android.content.SharedPreferences

/** No navegador não há Keystore: as preferências ficam no localStorage do próprio site. */
object EncryptedSharedPreferences {
    enum class PrefKeyEncryptionScheme { AES256_SIV }
    enum class PrefValueEncryptionScheme { AES256_GCM }

    fun create(
        context: Context,
        fileName: String,
        masterKey: MasterKey,
        keyEncryptionScheme: PrefKeyEncryptionScheme,
        valueEncryptionScheme: PrefValueEncryptionScheme
    ): SharedPreferences = context.getSharedPreferences(fileName, 0)
}

class MasterKey private constructor() {
    enum class KeyScheme { AES256_GCM }

    class Builder(context: Context) {
        private var scheme: KeyScheme = KeyScheme.AES256_GCM
        fun setKeyScheme(value: KeyScheme): Builder { scheme = value; return this }
        fun build(): MasterKey = MasterKey()
    }
}
