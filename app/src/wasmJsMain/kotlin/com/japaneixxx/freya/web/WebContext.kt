package com.japaneixxx.freya.web

import androidx.fragment.app.FragmentActivity

/** Contexto único da aplicação web (equivalente ao applicationContext do Android). */
object WebContext {
    val current: FragmentActivity = FragmentActivity()
}
