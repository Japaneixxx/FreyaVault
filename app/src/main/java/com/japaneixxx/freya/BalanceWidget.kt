package com.japaneixxx.freya

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import com.japaneixxx.freya.data.Settings
import com.japaneixxx.freya.ui.accentColors
import com.japaneixxx.freya.ui.linkTarget
import com.japaneixxx.freya.ui.moneyPlain
import com.japaneixxx.freya.R

/**
 * Widget de saldo configurável (conteúdo, estilo e destino do toque, por widget).
 * Com bloqueio ou "ocultar valores" ativos, não mostra números na tela inicial.
 */
class BalanceWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, it)) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val s = Settings(context)
        appWidgetIds.forEach { s.deleteWidgetConfig(it) }
    }

    companion object {
        private const val LIGHT_TEXT = 0xFFFFFFFF.toInt()
        private const val LIGHT_SUB = 0xCCDDE7F2.toInt()
        private const val DARK_TEXT = 0xFF1F2933.toInt()
        private const val DARK_SUB = 0xFF5B6773.toInt()

        private fun views(ctx: Context, id: Int): RemoteViews {
            val s = Settings(ctx)
            val cfg = s.widgetConfig(id)
            val hide = s.hideValues || s.lockEnabled
            val rv = RemoteViews(ctx.packageName, R.layout.widget_balance)

            val (title, value, sub) = when (cfg.show) {
                "BANK" -> Triple("Saldo em contas", s.widgetBank, null)
                "CARDS" -> Triple("Fatura dos cartões", s.widgetCards, null)
                else -> Triple("Saldo em contas", s.widgetBank, "Cartões (fatura): ${moneyPlain(s.widgetCards)}")
            }
            rv.setTextViewText(R.id.w_title, title)
            rv.setTextViewText(R.id.w_balance, if (hide) "R$ ••••" else moneyPlain(value))
            rv.setTextViewText(R.id.w_sub, if (hide || sub == null) "" else sub)

            val lightBg = cfg.style == "LIGHT"
            rv.setTextColor(R.id.w_title, if (lightBg) DARK_SUB else LIGHT_SUB)
            rv.setTextColor(R.id.w_balance, if (lightBg) DARK_TEXT else LIGHT_TEXT)
            rv.setTextColor(R.id.w_sub, if (lightBg) DARK_SUB else LIGHT_SUB)
            when (cfg.style) {
                "DARK" -> rv.setInt(R.id.w_root, "setBackgroundResource", R.drawable.widget_bg_dark)
                "LIGHT" -> rv.setInt(R.id.w_root, "setBackgroundResource", R.drawable.widget_bg_light)
                "CLEAR" -> rv.setInt(R.id.w_root, "setBackgroundResource", R.drawable.widget_bg_clear)
                else -> {
                    rv.setInt(R.id.w_root, "setBackgroundResource", R.drawable.widget_bg)
                    // A cor de destaque só pinta o widget no Android 12+, onde o RemoteViews aceita tonalidade.
                    val accent = s.accentColor
                    if (accent != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        rv.setColorStateList(
                            R.id.w_root, "setBackgroundTintList",
                            ColorStateList.valueOf(accentColors(accent, false).primary)
                        )
                    }
                }
            }

            val pi = PendingIntent.getActivity(
                ctx, id, Shortcuts.intentFor(ctx, linkTarget(cfg.opens).link),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            return rv
        }

        fun update(ctx: Context, id: Int) {
            AppWidgetManager.getInstance(ctx).updateAppWidget(id, views(ctx, id))
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, BalanceWidget::class.java))
            ids.forEach { mgr.updateAppWidget(it, views(ctx, it)) }
        }
    }
}
