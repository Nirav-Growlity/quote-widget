package io.github.niravgrowlity.quotewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViewsService
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews

class QuoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { WidgetRenderer.update(ctx, mgr, it) }
        Rotation.schedule(ctx, force = false)
    }

    /** Called when the user resizes the widget: re-fit the text to the new size. */
    override fun onAppWidgetOptionsChanged(ctx: Context, mgr: AppWidgetManager, id: Int, options: Bundle) {
        WidgetRenderer.update(ctx, mgr, id)
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

/** Feeds the single long-quote item to the scrollable list on Android 8–11. */
class QuoteListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val ctx: Context) : RemoteViewsFactory {
        override fun onCreate() = Unit
        override fun onDataSetChanged() = Unit
        override fun onDestroy() = Unit
        override fun getCount() = if (QuoteStore.current(ctx) != null) 1 else 0
        override fun getViewAt(position: Int): RemoteViews =
            WidgetRenderer.scrollItem(ctx, QuoteStore.current(ctx)?.text ?: "")
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = WidgetRenderer.ITEM_VIEW_TYPES
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}

object WidgetRenderer {
    // Must match the padding / meta line in the widget layouts.
    private const val PADDING_DP = 16f
    private const val META_DP = 22f
    private const val LINE_SPACING = 1.1f
    /** Smallest size we'll shrink to. Below this the quote scrolls instead, so it stays readable. */
    const val READABLE_SP = 13f
    private const val MAX_SP = 30f
    /** Distinct item layouts across all styles/themes (see WidgetStyle.items). */
    const val ITEM_VIEW_TYPES = 10

    private fun component(ctx: Context) = ComponentName(ctx, QuoteWidgetProvider::class.java)

    fun hasWidgets(ctx: Context) =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(component(ctx)).isNotEmpty()

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        mgr.getAppWidgetIds(component(ctx)).forEach { update(ctx, mgr, it) }
    }

    /** Each widget is rendered for its own size, so two widgets of different sizes both fit. */
    fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val o = mgr.getAppWidgetOptions(id)
        val portrait = ctx.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        // Launchers report: portrait = minWidth x maxHeight, landscape = maxWidth x minHeight.
        val w = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val h = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        mgr.updateAppWidget(id, build(ctx, w.takeIf { it > 0 } ?: 250, h.takeIf { it > 0 } ?: 110, id))
        if (Build.VERSION.SDK_INT < 31) {
            @Suppress("DEPRECATION")
            mgr.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }
    }

    /** The quote as one item in the scrollable list. Tapping it still goes to the next quote. */
    fun scrollItem(ctx: Context, text: String): RemoteViews {
        val item = RemoteViews(ctx.packageName, QuoteStore.style(ctx).item(QuoteStore.themeMode(ctx)))
        item.setTextViewText(R.id.widget_item_text, text)
        item.setTextViewTextSize(R.id.widget_item_text, TypedValue.COMPLEX_UNIT_SP, READABLE_SP)
        item.setInt(
            R.id.widget_item_text, "setGravity",
            if (QuoteStore.align(ctx) == Align.CENTER) Gravity.CENTER_HORIZONTAL else Gravity.START,
        )
        item.setOnClickFillInIntent(R.id.widget_item_text, Intent())
        return item
    }

    fun build(ctx: Context, widthDp: Int, heightDp: Int, widgetId: Int = 0): RemoteViews {
        val style = QuoteStore.style(ctx)
        val rv = RemoteViews(ctx.packageName, style.layout(QuoteStore.themeMode(ctx)))
        val openApp = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val rotation = QuoteStore.rotation(ctx)
        val q = QuoteStore.current(ctx)
        if (q == null) {
            rv.setTextViewText(R.id.widget_text, ctx.getString(R.string.widget_empty))
            rv.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, 14f)
            rv.setViewVisibility(R.id.widget_meta, View.GONE)
            rv.setOnClickPendingIntent(R.id.widget_text, openApp)
            return rv
        }

        val maxSp = (MAX_SP * QuoteStore.textSize(ctx).scale).coerceAtLeast(READABLE_SP)
        val sp = fitTextSize(
            ctx, q.text,
            widthDp - 2 * PADDING_DP,
            heightDp - 2 * PADDING_DP - META_DP,
            maxSp, style.bold,
        )
        val scrolling = sp == null
        val next = Rotation.intent(ctx, Rotation.ACTION_NEXT)

        if (!scrolling) {
            rv.setViewVisibility(R.id.widget_list, View.GONE)
            rv.setViewVisibility(R.id.widget_text, View.VISIBLE)
            rv.setTextViewText(R.id.widget_text, q.text)
            rv.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, sp!!)
            rv.setInt(
                R.id.widget_text, "setGravity",
                if (QuoteStore.align(ctx) == Align.CENTER) Gravity.CENTER
                else Gravity.START or Gravity.CENTER_VERTICAL,
            )
            rv.setOnClickPendingIntent(R.id.widget_text, next)
        } else {
            // Too long to fit at a readable size: show it at READABLE_SP in a scrollable list.
            rv.setViewVisibility(R.id.widget_text, View.GONE)
            rv.setViewVisibility(R.id.widget_list, View.VISIBLE)
            if (Build.VERSION.SDK_INT >= 31) {
                rv.setRemoteAdapter(
                    R.id.widget_list,
                    RemoteViews.RemoteCollectionItems.Builder()
                        .addItem(q.id, scrollItem(ctx, q.text))
                        .setViewTypeCount(ITEM_VIEW_TYPES)
                        .build(),
                )
            } else {
                val svc = Intent(ctx, QuoteListService::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
                @Suppress("DEPRECATION")
                rv.setRemoteAdapter(R.id.widget_list, svc)
            }
            // List items can't have their own click intents; they fill in this template.
            rv.setPendingIntentTemplate(
                R.id.widget_list,
                PendingIntent.getBroadcast(
                    ctx, 7, Intent(ctx, RotationReceiver::class.java).setAction(Rotation.ACTION_NEXT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                ),
            )
        }

        val pos = Math.floorMod(QuoteStore.index(ctx), rotation.size) + 1
        val meta = buildString {
            if (scrolling) append("↕ scroll   ")
            if (rotation.any { it.pinned }) append("★ ")
            if (QuoteStore.showCounter(ctx)) append("$pos / ${rotation.size}")
        }.trim()
        rv.setTextViewText(R.id.widget_meta, meta.ifEmpty { "⋯" })
        rv.setViewVisibility(R.id.widget_meta, View.VISIBLE)

        // Tap the quote = next quote. Tap the small corner text = open the app.
        rv.setOnClickPendingIntent(R.id.widget_meta, openApp)
        return rv
    }

    /**
     * Largest text size (sp) at which the whole quote fits in the given box, found by binary
     * search with the same line-breaking the TextView uses. Returns null if it doesn't fit even
     * at READABLE_SP (then the widget scrolls). Runs only when a widget redraws.
     */
    private fun fitTextSize(ctx: Context, text: String, wDp: Float, hDp: Float, maxSp: Float, bold: Boolean): Float? {
        val dm = ctx.resources.displayMetrics
        val widthPx = (wDp * dm.density).toInt()
        // 5% safety margin: launcher fonts can differ slightly from ours.
        val heightPx = hDp * dm.density * 0.95f
        if (widthPx <= 0 || heightPx <= 0) return null
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        fun fits(sp: Float): Boolean {
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm)
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, LINE_SPACING)
                .setIncludePad(true)
                .build()
            return layout.height <= heightPx
        }
        if (!fits(READABLE_SP)) return null
        var lo = READABLE_SP
        var hi = maxSp
        if (fits(hi)) return hi
        repeat(8) {
            val mid = (lo + hi) / 2
            if (fits(mid)) lo = mid else hi = mid
        }
        return lo
    }
}
