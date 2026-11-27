package com.japaneixxx.freya

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.japaneixxx.freya.data.Db
import com.japaneixxx.freya.data.Settings
import com.japaneixxx.freya.ui.calendarWishes
import com.japaneixxx.freya.ui.moneyPlain
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lembretes de fatura (e, opcionalmente, contas a pagar) na agenda do celular.
 * Usa um calendário local "Freya" (não sobe para a conta Google). Se o aparelho não deixar criá-lo, cai para
 * um calendário editável existente e avisa na tela.
 */
object CalendarSync {
    val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

    private const val ACCOUNT = "Freya"
    private const val REMOVED_BY_USER = -1L
    private const val HOUR = 9
    private val lock = Mutex()

    data class Report(val created: Int, val removed: Int, val total: Int, val calendar: String)

    fun hasPermission(ctx: Context): Boolean =
        PERMISSIONS.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

    /** Cria, atualiza e remove os eventos para refletir os vencimentos atuais. Lança SecurityException sem permissão. */
    suspend fun sync(ctx: Context): Report? = lock.withLock {
        val settings = Settings(ctx)
        if (!settings.calendarEnabled) return@withLock null
        if (!hasPermission(ctx)) throw SecurityException("Sem permissão da agenda")
        val dao = Db.get(ctx).dao()
        val hide = settings.hideValues || settings.lockEnabled
        val amountText: ((Double) -> String)? = if (settings.calendarShowAmount && !hide) ::moneyPlain else null
        val wishes = calendarWishes(
            dao.accountsNow(), dao.billsNow(), dao.billPaymentsNow(), settings.accountAliases(),
            settings.calendarBills, amountText
        )
        val calId = ensureCalendar(ctx, settings)
        val map = settings.calendarEvents().toMutableMap()
        val existing = existingEventIds(ctx, calId, map.values.filter { it > 0 })
        val today = LocalDate.now()
        var created = 0
        var removed = 0

        wishes.forEach { w ->
            val id: Long? = map[w.key]
            if (id == null) {
                insertEvent(ctx, calId, w.date, w.title, w.description, settings.calendarLeadDays)?.let {
                    map[w.key] = it; created++
                }
            } else if (id != REMOVED_BY_USER) {
                if (id in existing) updateEvent(ctx, id, w.date, w.title, w.description, settings.calendarLeadDays)
                else map[w.key] = REMOVED_BY_USER // o usuário apagou na agenda: não recria
            }
        }
        val wanted = wishes.map { it.key }.toSet()
        map.keys.filter { it !in wanted }.forEach { key ->
            val id = map.getValue(key)
            val date = keyDate(key)
            // Vencimento que já passou fica na agenda como histórico; só os futuros que deixaram de valer saem.
            if (id > 0 && id in existing && date != null && !date.isBefore(today)) {
                deleteEvent(ctx, id); removed++
            }
            map.remove(key)
        }
        settings.saveCalendarEvents(map)
        Report(created, removed, wishes.size, settings.calendarLabel)
    }

    /** Desligou a integração: remove os lembretes futuros criados pelo Freya (os passados ficam). */
    suspend fun clear(ctx: Context): Unit = lock.withLock {
        val settings = Settings(ctx)
        if (hasPermission(ctx)) {
            val map = settings.calendarEvents()
            val calId = settings.calendarId
            if (calId > 0) {
                val existing = existingEventIds(ctx, calId, map.values.filter { it > 0 })
                val today = LocalDate.now()
                map.forEach { (key, id) ->
                    val date = keyDate(key)
                    if (id > 0 && id in existing && date != null && !date.isBefore(today)) deleteEvent(ctx, id)
                }
            }
        }
        settings.saveCalendarEvents(emptyMap())
    }

    private fun keyDate(key: String): LocalDate? = runCatching { LocalDate.parse(key.takeLast(10)) }.getOrNull()

    private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    private fun calendarExists(ctx: Context, id: Long): Boolean =
        ctx.contentResolver.query(
            ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, id),
            arrayOf(CalendarContract.Calendars._ID), null, null, null
        )?.use { it.moveToFirst() } == true

    private fun ensureCalendar(ctx: Context, settings: Settings): Long {
        val saved = settings.calendarId
        if (saved > 0 && calendarExists(ctx, saved)) return saved

        // 1) calendário local do Freya (já existente ou novo)
        ctx.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(ACCOUNT, CalendarContract.ACCOUNT_TYPE_LOCAL), null
        )?.use { c -> if (c.moveToFirst()) return c.getLong(0).also { settings.calendarId = it; settings.calendarLabel = "Freya (só neste aparelho)" } }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "freya")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Freya")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF3F5F8A.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, ZoneId.systemDefault().id)
        }
        val created = runCatching {
            ctx.contentResolver.insert(asSyncAdapter(CalendarContract.Calendars.CONTENT_URI), values)?.let { ContentUris.parseId(it) }
        }.getOrNull()
        if (created != null && created > 0) {
            settings.calendarId = created
            settings.calendarLabel = "Freya (só neste aparelho)"
            return created
        }

        // 2) plano B: primeiro calendário editável do aparelho (pode sincronizar com a conta Google)
        ctx.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )?.use { c ->
            if (c.moveToFirst()) {
                settings.calendarId = c.getLong(0)
                settings.calendarLabel = "${c.getString(1)} (${c.getString(2)}, pode sincronizar com a conta)"
                return c.getLong(0)
            }
        }
        throw IllegalStateException("Nenhum calendário disponível no aparelho")
    }

    private fun existingEventIds(ctx: Context, calId: Long, ids: List<Long>): Set<Long> {
        if (ids.isEmpty()) return emptySet()
        val out = mutableSetOf<Long>()
        ctx.contentResolver.query(
            CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0",
            arrayOf(calId.toString()), null
        )?.use { c -> while (c.moveToNext()) out += c.getLong(0) }
        return out.intersect(ids.toSet())
    }

    private fun eventValues(date: LocalDate, title: String, description: String): ContentValues {
        val zone = ZoneId.systemDefault()
        val start = date.atTime(HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        return ContentValues().apply {
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, start + 30 * 60_000L)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
    }

    private fun setReminders(ctx: Context, eventId: Long, leadDays: Int) {
        ctx.contentResolver.delete(
            CalendarContract.Reminders.CONTENT_URI, "${CalendarContract.Reminders.EVENT_ID} = ?", arrayOf(eventId.toString())
        )
        val minutes = buildList { if (leadDays > 0) add(leadDays * 24 * 60); add(0) }
        minutes.forEach { m ->
            ctx.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, m)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            })
        }
    }

    private fun insertEvent(ctx: Context, calId: Long, date: LocalDate, title: String, description: String, lead: Int): Long? {
        val values = eventValues(date, title, description).apply { put(CalendarContract.Events.CALENDAR_ID, calId) }
        val id = ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.let { ContentUris.parseId(it) } ?: return null
        setReminders(ctx, id, lead)
        return id
    }

    private fun updateEvent(ctx: Context, id: Long, date: LocalDate, title: String, description: String, lead: Int) {
        ctx.contentResolver.update(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), eventValues(date, title, description), null, null
        )
        setReminders(ctx, id, lead)
    }

    private fun deleteEvent(ctx: Context, id: Long) {
        ctx.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), null, null)
    }
}
