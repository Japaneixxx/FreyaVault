package com.japaneixxx.freya

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.japaneixxx.freya.ui.DeepLink
import com.japaneixxx.freya.ui.SHORTCUTS

/** Atalhos do ícone do app e destinos abertos pelo widget. Tudo passa por extras da Intent que a MainActivity entende. */
object Shortcuts {
    private const val EXTRA_TAB = "freya_tab"
    private const val EXTRA_PLAN = "freya_plan"
    private const val EXTRA_ACTION = "freya_action"

    fun intentFor(ctx: Context, link: DeepLink): Intent =
        Intent(ctx, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_TAB, link.tab)
            link.planSection?.let { putExtra(EXTRA_PLAN, it) }
            link.action?.let { putExtra(EXTRA_ACTION, it) }
        }

    fun linkFrom(intent: Intent?): DeepLink? {
        if (intent == null || !intent.hasExtra(EXTRA_TAB)) return null
        return DeepLink(
            tab = intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, 4),
            planSection = if (intent.hasExtra(EXTRA_PLAN)) intent.getIntExtra(EXTRA_PLAN, 0) else null,
            action = intent.getStringExtra(EXTRA_ACTION)
        )
    }

    /** Publica os atalhos escolhidos (os que ficam ao segurar o ícone do app). Lista vazia remove todos. */
    fun publish(ctx: Context, ids: List<String>) {
        runCatching {
            val list = ids.mapNotNull { id -> SHORTCUTS.firstOrNull { it.id == id } }.mapIndexed { i, d ->
                ShortcutInfoCompat.Builder(ctx, "freya_${d.id}")
                    .setShortLabel(d.label)
                    .setLongLabel(d.label)
                    .setIcon(IconCompat.createWithResource(ctx, R.drawable.ic_shortcut))
                    .setIntent(intentFor(ctx, d.link))
                    .setRank(i)
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(ctx, list)
        }
    }
}
