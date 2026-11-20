package com.japaneixxx.freya.data

import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Senha errada ou arquivo adulterado (o AES-GCM detecta os dois casos). */
class BadBackupPasswordException : Exception("Senha incorreta ou arquivo de backup corrompido")

/** Criptografia opcional do backup: AES-256-GCM com chave derivada da senha por PBKDF2-HMAC-SHA256. */
object BackupCrypto {
    private const val ITERATIONS = 210_000
    private const val MARKER = "freya_backup"

    fun isEncrypted(text: String): Boolean =
        runCatching { JSONObject(text).optString(MARKER) == "encrypted" }.getOrDefault(false)

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(key, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(plain: String, password: CharArray): String {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, ITERATIONS), GCMParameterSpec(128, iv))
        val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val b64 = Base64.getEncoder()
        return JSONObject()
            .put(MARKER, "encrypted").put("v", 1).put("iter", ITERATIONS)
            .put("salt", b64.encodeToString(salt)).put("iv", b64.encodeToString(iv))
            .put("data", b64.encodeToString(data))
            .toString()
    }

    @Throws(BadBackupPasswordException::class)
    fun decrypt(text: String, password: CharArray): String {
        try {
            val o = JSONObject(text)
            val b64 = Base64.getDecoder()
            val salt = b64.decode(o.getString("salt"))
            val iv = b64.decode(o.getString("iv"))
            val data = b64.decode(o.getString("data"))
            val iterations = o.optInt("iter", ITERATIONS).coerceIn(10_000, 2_000_000)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(128, iv))
            return String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw BadBackupPasswordException()
        } catch (e: IllegalArgumentException) {
            throw BadBackupPasswordException()
        } catch (e: org.json.JSONException) {
            throw BadBackupPasswordException()
        }
    }
}
