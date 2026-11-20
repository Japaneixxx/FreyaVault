package com.japaneixxx.freya.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hash do PIN: PBKDF2 (lento de propósito, pois um PIN tem poucas combinações). */
object PinHasher {
    const val ITERATIONS = 120_000

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun pbkdf2(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        try {
            val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(key)
        } finally {
            spec.clearPassword()
        }
    }

    /** Formato usado até a 1.8.5; só serve para reconhecer um PIN antigo e migrá-lo no primeiro desbloqueio. */
    fun legacySha256(pin: String, salt: ByteArray): String =
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray(Charsets.UTF_8)))

    fun same(a: String, b: String): Boolean = MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    /** Espera após [failures] erros seguidos: 5 erros = 30s, depois dobra até 15 min. */
    fun lockDelayMs(failures: Int): Long =
        if (failures < 5) 0L else minOf(30_000L shl (failures - 5).coerceAtMost(5), 15 * 60_000L)
}
