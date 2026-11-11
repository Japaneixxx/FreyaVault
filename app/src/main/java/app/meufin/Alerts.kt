package app.meufin

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import app.meufin.data.Db
import app.meufin.data.Settings
import app.meufin.ui.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.concurrent.TimeUnit

/** Alertas locais: orçamentos, contas a pagar e vencimento de fatura. Sem servidor. */
object Alerts {
    private const val CHANNEL = "alerts"

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (!canNotify(ctx)) return
        try {
            val nm = NotificationManagerCompat.from(ctx)
            nm.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("Alertas financeiros").build()
            )
            val pi = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    /** Roda após cada sincronização e uma vez por dia (WorkManager, sem rede). */
    suspend fun check(ctx: Context) {
        val settings = Settings(ctx)
        if (!settings.alertsEnabled) return
        val dao = Db.get(ctx).dao()
        val hide = settings.hideValues || settings.lockEnabled
        fun m(v: Double) = if (hide) "••••" else moneyPlain(v)

        val today = LocalDate.now()
        val month = YearMonth.now().toString()

        // 1) orçamentos
        val items = buildItems(dao.txNow(), dao.rulesNow(), dao.metasNow())
        val spent = spendByCategory(items.filter { !it.ignored }.inMonth(month)).toMap()
        dao.budgetsNow().forEach { b ->
            val s = spent[b.category] ?: 0.0
            val pct = if (b.limitAmount > 0) s / b.limitAmount else 0.0
            val level = when { pct >= 1.0 -> 100; pct >= 0.8 -> 80; else -> 0 }
            if (level > 0 && !settings.seen("b_${b.category}_${month}_$level")) {
                settings.markSeen("b_${b.category}_${month}_$level")
                val title = if (level == 100) "Orçamento estourado: ${b.category}" else "Orçamento quase no limite: ${b.category}"
                notify(ctx, ("b" + b.category + month + level).hashCode(), title, "Você gastou ${m(s)} de ${m(b.limitAmount)} neste mês.")
            }
        }

        // 2) contas a pagar
        dao.billsNow().filter { it.lastPaidMonth != month }.forEach { b ->
            val due = YearMonth.now().atDay(minOf(b.dueDay, YearMonth.now().lengthOfMonth()))
            val days = Duration.between(today.atStartOfDay(), due.atStartOfDay()).toDays().toInt()
            if (days in -7..3 && !settings.seen("bill_${b.id}_$today")) {
                settings.markSeen("bill_${b.id}_$today")
                val whenTxt = when {
                    days < 0 -> "venceu há ${-days} dia(s)"
                    days == 0 -> "vence hoje"
                    else -> "vence em $days dia(s)"
                }
                notify(ctx, ("bill" + b.id + today).hashCode(), "Conta: ${b.name}", "${b.name} $whenTxt (${m(b.amount)}).")
            }
        }

        // 3) fatura do cartão
        dao.accountsNow().filter { it.type == "CREDIT" && it.dueDate != null }.forEach { a ->
            val due = runCatching { LocalDate.parse(a.dueDate) }.getOrNull() ?: return@forEach
            val days = Duration.between(today.atStartOfDay(), due.atStartOfDay()).toDays().toInt()
            if (days in 0..3 && !settings.seen("card_${a.id}_${a.dueDate}_$today")) {
                settings.markSeen("card_${a.id}_${a.dueDate}_$today")
                val whenTxt = if (days == 0) "vence hoje" else "vence em $days dia(s)"
                notify(ctx, ("card" + a.id + today).hashCode(), "Fatura ${a.institution}", "A fatura do cartão ${a.name} $whenTxt (${m(a.balance)}).")
            }
        }
    }

    fun schedule(ctx: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (!enabled) {
            wm.cancelUniqueWork("daily-alerts")
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(9, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMinutes()
        val req = PeriodicWorkRequestBuilder<DailyWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork("daily-alerts", ExistingPeriodicWorkPolicy.UPDATE, req)
    }
}

class DailyWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        runCatching { Alerts.check(applicationContext) }
        return Result.success()
    }
}
