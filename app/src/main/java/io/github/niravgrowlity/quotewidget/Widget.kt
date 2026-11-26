package io.github.niravgrowlity.quotewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews

class QuoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        mgr.updateAppWidget(ids, WidgetRenderer.build(ctx))
        Rotation.schedule(ctx, force = false)
    }

    override fun onEnabled(ctx: Context) = Rotation.schedule(ctx, force = true)
    override fun onDisabled(ctx: Context) = Rotation.cancel(ctx)
}

/** Handles the rotation timer and taps on the widget. */
class RotationReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Rotation.ACTION_TICK, Rotation.ACTION_NEXT -> Rotation.advance(ctx)
        }
    }
}

/** Alarms are wiped on reboot and on app update, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        WidgetRenderer.updateAll(ctx)
        Rotation.schedule(ctx, force = true)
    }
}

object Rotation {
    const val ACTION_TICK = "io.github.niravgrowlity.quotewidget.TICK"
    const val ACTION_NEXT = "io.github.niravgrowlity.quotewidget.NEXT"

    fun intent(ctx: Context, action: String, flags: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? =
        PendingIntent.getBroadcast(
            ctx, action.hashCode(),
            Intent(ctx, RotationReceiver::class.java).setAction(action),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * Arms the timer for the next quote change. With force=false an already-armed timer is left
     * alone, so opening the app doesn't keep pushing the next change back.
     */
    fun schedule(ctx: Context, force: Boolean) {
        if (!WidgetRenderer.hasWidgets(ctx)) { cancel(ctx); return }
        if (!force && intent(ctx, ACTION_TICK, PendingIntent.FLAG_NO_CREATE) != null) return
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = intent(ctx, ACTION_TICK)!!
        am.cancel(pi)
        // Non-wakeup alarm: if the phone is asleep it fires the next time the screen comes on.
        am.set(AlarmManager.RTC, System.currentTimeMillis() + QuoteStore.periodMinutes(ctx) * 60_000L, pi)
    }

    fun cancel(ctx: Context) {
        intent(ctx, ACTION_TICK, PendingIntent.FLAG_NO_CREATE)?.let {
            ctx.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    /** Next quote, then restart the timer so each quote gets a full period. */
    fun advance(ctx: Context) {
        QuoteStore.advance(ctx)
        WidgetRenderer.updateAll(ctx)
        schedule(ctx, force = true)
    }
}

object WidgetRenderer {
    private fun component(ctx: Context) = ComponentName(ctx, QuoteWidgetProvider::class.java)

    fun hasWidgets(ctx: Context) =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(component(ctx)).isNotEmpty()

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(component(ctx))
        if (ids.isNotEmpty()) mgr.updateAppWidget(ids, build(ctx))
    }

    fun build(ctx: Context): RemoteViews {
        val style = QuoteStore.style(ctx)
        val rv = RemoteViews(ctx.packageName, style.layout)
        val openApp = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val rotation = QuoteStore.rotation(ctx)
        val q = QuoteStore.current(ctx)
        if (q == null) {
            rv.setTextViewText(R.id.widget_text, ctx.getString(R.string.widget_empty))
            rv.setViewVisibility(R.id.widget_meta, View.GONE)
            rv.setOnClickPendingIntent(R.id.widget_text, openApp)
            return rv
        }

        val base = when {
            q.text.length > 220 -> 13f
            q.text.length > 120 -> 15f
            else -> 18f
        }
        rv.setTextViewText(R.id.widget_text, q.text)
        rv.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, base * QuoteStore.textSize(ctx).scale)
        rv.setInt(
            R.id.widget_text, "setGravity",
            if (QuoteStore.align(ctx) == Align.CENTER) Gravity.CENTER
            else Gravity.START or Gravity.CENTER_VERTICAL,
        )

        val pos = Math.floorMod(QuoteStore.index(ctx), rotation.size) + 1
        val pinnedMode = rotation.any { it.pinned }
        val meta = buildString {
            if (pinnedMode) append("★ ")
            if (QuoteStore.showCounter(ctx)) append("$pos / ${rotation.size}")
        }.trim()
        rv.setTextViewText(R.id.widget_meta, meta.ifEmpty { "⋯" })
        rv.setViewVisibility(R.id.widget_meta, View.VISIBLE)

        // Tap the quote = next quote. Tap the small corner text = open the app.
        rv.setOnClickPendingIntent(R.id.widget_text, Rotation.intent(ctx, Rotation.ACTION_NEXT))
        rv.setOnClickPendingIntent(R.id.widget_meta, openApp)
        return rv
    }
}
