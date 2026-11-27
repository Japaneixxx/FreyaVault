package com.japaneixxx.freya

import android.content.Context

/** Notificações do sistema não existem no navegador. */
object Alerts {
    fun canNotify(ctx: Context): Boolean = false
    fun notificationsEnabled(ctx: Context): Boolean = true
    fun openNotificationSettings(ctx: Context): Boolean = false
    suspend fun check(ctx: Context) {}
    fun schedule(ctx: Context, enabled: Boolean) {}
}
