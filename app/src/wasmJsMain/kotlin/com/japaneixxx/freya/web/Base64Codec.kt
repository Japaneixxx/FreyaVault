package com.japaneixxx.freya.web

/** Base64 puro Kotlin, usado no navegador (não há java.util.Base64 no Wasm). */
object Base64Codec {
    private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(bytes: ByteArray): String = buildString {
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xff
            val has1 = i + 1 < bytes.size
            val has2 = i + 2 < bytes.size
            val b1 = if (has1) bytes[i + 1].toInt() and 0xff else 0
            val b2 = if (has2) bytes[i + 2].toInt() and 0xff else 0
            append(CHARS[b0 shr 2])
            append(CHARS[((b0 and 3) shl 4) or (b1 shr 4)])
            append(if (has1) CHARS[((b1 and 15) shl 2) or (b2 shr 6)] else '=')
            append(if (has2) CHARS[b2 and 63] else '=')
            i += 3
        }
    }

    fun decode(text: String): ByteArray {
        val clean = text.trim().filter { it != '\n' && it != '\r' && it != ' ' && it != '\t' }
        val out = ByteArray(clean.length * 3 / 4 + 3)
        var outIndex = 0
        var i = 0
        while (i + 1 < clean.length) {
            val c0 = valueOf(clean[i])
            val c1 = valueOf(clean[i + 1])
            val c2 = if (i + 2 < clean.length) valueOf(clean[i + 2]) else -1
            val c3 = if (i + 3 < clean.length) valueOf(clean[i + 3]) else -1
            if (c0 < 0 || c1 < 0) break
            if (outIndex < out.size) out[outIndex++] = ((c0 shl 2) or (c1 shr 4)).toByte()
            if (c2 >= 0 && outIndex < out.size) out[outIndex++] = (((c1 and 15) shl 4) or (c2 shr 2)).toByte()
            if (c3 >= 0 && outIndex < out.size) out[outIndex++] = (((c2 and 3) shl 6) or c3).toByte()
            i += 4
        }
        return out.copyOf(outIndex)
    }

    private fun valueOf(c: Char): Int = CHARS.indexOf(c)
}
