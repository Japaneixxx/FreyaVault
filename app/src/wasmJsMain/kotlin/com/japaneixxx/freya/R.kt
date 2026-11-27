package com.japaneixxx.freya

/** Recursos usados pela interface compartilhada (no Android vêm de res/values). */
object R {
    object string {
        const val developer_name = 1

        fun of(id: Int): String = when (id) {
            developer_name -> "Japaneixxx"
            else -> ""
        }
    }
}
