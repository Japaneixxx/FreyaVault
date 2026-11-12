package app.meufin.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ptBR = Locale("pt", "BR")
private val brl: NumberFormat = NumberFormat.getCurrencyInstance(ptBR)
private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBR)

/** Modo privacidade: esconde todos os valores do app. */
object Privacy { var hidden by mutableStateOf(false) }

fun moneyPlain(v: Double): String = brl.format(v)

fun money(v: Double): String = if (Privacy.hidden) "R$ ••••" else brl.format(v)

fun monthLabel(ym: String): String {
    val d = YearMonth.parse(ym)
    return d.month.getDisplayName(TextStyle.FULL, ptBR).replaceFirstChar { it.uppercase() } + " " + d.year
}

fun shortMonth(ym: YearMonth): String =
    ym.month.getDisplayName(TextStyle.SHORT, ptBR).replace(".", "").replaceFirstChar { it.uppercase() }

fun dayLabel(iso: String): String =
    runCatching { LocalDate.parse(iso).format(dayFmt).replaceFirstChar { it.uppercase() } }.getOrDefault(iso)

fun ago(ms: Long): String {
    if (ms == 0L) return "nunca"
    val m = (System.currentTimeMillis() - ms) / 60000
    return when {
        m < 1 -> "agora"
        m < 60 -> "há $m min"
        m < 1440 -> "há ${m / 60} h"
        else -> "há ${m / 1440} d"
    }
}

fun weekdayName(d: DayOfWeek): String = d.getDisplayName(TextStyle.FULL, ptBR)
<<<<<<< HEAD

fun pct(v: Double): String = String.format(ptBR, "%.2f%%", v)
=======
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
