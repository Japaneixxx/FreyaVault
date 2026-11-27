package com.japaneixxx.freya.data

import com.japaneixxx.freya.web.Base64Codec
import com.japaneixxx.freya.web.Sha256
import kotlin.random.Random

object PinHasher {
    /** Menos iterações que no Android para não travar o navegador (AINDA lento de propósito). */
    const val ITERATIONS = 20_000

    fun newSalt(): ByteArray = ByteArray(16) { Random.nextBytes(1)[0] }

    fun pbkdf2(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): String {
        val password = pin.encodeToByteArray()
        val input = salt + byteArrayOf(0, 0, 0, 1)
        var u = Sha256.hmacSha256(password, input)
        val derived = u.copyOf()
        for (i in 2..iterations) {
            u = Sha256.hmacSha256(password, u)
            for (j in derived.indices) derived[j] = (derived[j].toInt() xor u[j].toInt()).toByte()
        }
        return Base64Codec.encode(derived)
    }

    fun legacySha256(pin: String, salt: ByteArray): String =
        Base64Codec.encode(Sha256.digest(salt + pin.encodeToByteArray()))

    fun same(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    fun lockDelayMs(failures: Int): Long =
        if (failures < 5) 0L else minOf(30_000L shl (failures - 5).coerceAtMost(5), 15 * 60_000L)
}
