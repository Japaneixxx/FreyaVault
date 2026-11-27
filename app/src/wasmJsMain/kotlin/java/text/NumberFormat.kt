package java.text

import java.util.Locale
import kotlin.math.abs

/** Formatador de moeda usado pela interface. No navegador só o real (pt-BR) é necessário. */
abstract class NumberFormat {
    abstract fun format(value: Double): String
    open fun format(value: Long): String = format(value.toDouble())

    companion object {
        fun getCurrencyInstance(locale: Locale = Locale.US): NumberFormat = CurrencyFormat(locale)
        fun getNumberInstance(locale: Locale = Locale.US): NumberFormat = NumberInstance(locale)
    }
}

private class CurrencyFormat(private val locale: Locale) : NumberFormat() {
    override fun format(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val cents = kotlin.math.floor(abs(value) * 100.0 + 0.5).toLong()
        val reais = cents / 100
        val centavos = cents % 100
        val digits = reais.toString().reversed().chunked(3).joinToString(".").reversed()
        val symbol = if (locale.language == "pt") "R$ " else if (locale.country == "BR") "R$ " else "$ "
        val decimal = if (locale.language == "pt") "," else "."
        return "$sign$symbol$digits$decimal${centavos.toString().padStart(2, '0')}"
    }
}

private class NumberInstance(private val locale: Locale) : NumberFormat() {
    override fun format(value: Double): String {
        val decimal = if (locale.language == "pt") "," else "."
        val text = value.toString()
        return if (decimal == ".") text else text.replace('.', ',')
    }
}
