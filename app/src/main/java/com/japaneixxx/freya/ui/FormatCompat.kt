package com.japaneixxx.freya.ui

import java.util.Locale

/** Ponto único de formatação com `String.format`, para poder ser substituído no alvo web. */
internal fun formatNumber(locale: Locale, pattern: String, vararg args: Any?): String =
    String.format(locale, pattern, *args)
