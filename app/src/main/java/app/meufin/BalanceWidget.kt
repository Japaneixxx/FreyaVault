package app.meufin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.meufin.data.Settings
import app.meufin.ui.moneyPlain

/** Widget de saldo. Com bloqueio ou "ocultar valores" ativos, não mostra números na tela inicial. */
class BalanceWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        private fun views(ctx: Context): RemoteViews {
            val s = Settings(ctx)
            val hide = s.hideValues || s.lockEnabled
            val rv = RemoteViews(ctx.packageName, R.layout.widget_balance)
            rv.setTextViewText(R.id.w_balance, if (hide) "R$ ••••" else moneyPlain(s.widgetBank))
            rv.setTextViewText(R.id.w_sub, if (hide) "" else "Cartões (fatura): ${moneyPlain(s.widgetCards)}")
            val pi = PendingIntent.getActivity(
                ctx, 1, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            rv.setOnClickPendingIntent(R.id.w_root, pi)
            return rv
        }

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, BalanceWidget::class.java))
            ids.forEach { mgr.updateAppWidget(it, views(ctx)) }
        }
    }
}
