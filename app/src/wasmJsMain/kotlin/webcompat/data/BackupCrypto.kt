package com.japaneixxx.freya.data

import org.json.JSONObject

class BadBackupPasswordException : Exception("Senha incorreta ou arquivo de backup corrompido")

object BackupCrypto {
    private const val MARKER = "freya_backup"

    fun isEncrypted(text: String): Boolean =
        runCatching { JSONObject(text).optString(MARKER) == "encrypted" }.getOrDefault(false)

    fun encrypt(plain: String, password: CharArray): String =
        throw UnsupportedOperationException("Backup com senha não está disponível na versão web. Salve o backup sem senha.")

    fun decrypt(text: String, password: CharArray): String = throw BadBackupPasswordException()
}
