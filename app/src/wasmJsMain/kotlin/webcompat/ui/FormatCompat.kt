package com.japaneixxx.freya.ui

import com.japaneixxx.freya.web.WebBridge
import java.util.Locale

/** Formatação numérica mínima para o Wasm (sem java.util.Formatter). */
internal fun formatNumber(locale: Locale, pattern: String, vararg args: Any?): String {
    val comma = locale.language == "pt"
    val sb = StringBuilder()
    var i = 0
    var argIndex = 0
    while (i < pattern.length) {
        if (pattern[i] != '%') {
            sb.append(pattern[i++])
            continue
        }
        if (i + 1 < pattern.length && pattern[i + 1] == '%') {
            sb.append('%'); i += 2; continue
        }
        var j = i + 1
        while (j < pattern.length && !"dfgeEoxXs".contains(pattern[j])) j++
        if (j >= pattern.length) { sb.append(pattern[i]); i++; continue }
        val spec = pattern.substring(i, j + 1)
        val arg = args.getOrNull(argIndex++)
        val decimals = Regex("\\.(\\d+)").find(spec)?.groupValues?.get(1)?.toIntOrNull()
        val text = when (pattern[j]) {
            'd', 'o', 'x', 'X' -> when (arg) {
                is Long -> arg.toString()
                is Int -> arg.toString()
                is Number -> arg.toLong().toString()
                else -> arg?.toString().orEmpty()
            }
            'f' -> {
                val value = (arg as? Number)?.toDouble() ?: 0.0
                val scale = 1_000_000L
                rounded(value, decimals ?: 6).let { v ->
                    val whole = kotlin.math.floor(v).toLong()
                    val frac = decimals?.let {
                        if (it > 0) {
                            val f = kotlin.math.round((v - whole) * scale).toLong()
                            val digits = f.toString().padStart(6, '0').take(it)
                            (if (comma) "," else ".") + digits
                        } else ""
                    } ?: ""
                    whole.toString() + frac
                }
            }
            'e', 'E', 'g', 'G' -> (arg as? Number)?.toString().orEmpty()
            else -> arg?.toString().orEmpty()
        }
        // Largura zero à esquerda, ex.: %02d.
        val width = Regex("""0(\d+)""").find(spec)?.groupValues?.get(1)?.toIntOrNull()
        if (width != null && pattern[j] in "doxX" && text.length < width) {
            sb.append("0".repeat(width - text.length))
        }
        sb.append(text)
        i = j + 1
    }
    return sb.toString()
}

private fun rounded(value: Double, decimals: Int): Double {
    val factor = WebBridge.pow(10.0, decimals.toDouble())
    return kotlin.math.floor(value * factor + 0.5) / factor
}

/** Equivalente a `"%.2f".format(v)` nos arquivos da mesma package. */
internal fun String.format(vararg args: Any?): String = formatNumber(Locale.US, this, *args)
