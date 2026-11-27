package com.japaneixxx.freya

import android.content.Context

object CalendarSync {
    data class Report(val created: Int, val removed: Int, val total: Int, val calendar: String)

    val PERMISSIONS: Array<String> = emptyArray()

    fun hasPermission(ctx: Context): Boolean = false
    suspend fun sync(ctx: Context): Report? = null
    suspend fun clear(ctx: Context) {}
}
