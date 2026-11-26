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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(applicationContext, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))

    private class Factory(private val ctx: Context, private val widgetId: Int) : RemoteViewsFactory {
        override fun onCreate() = Unit
        override fun onDataSetChanged() = Unit
        override fun onDestroy() = Unit
        override fun getCount() = if (QuoteStore.current(ctx) != null) 1 else 0
        override fun getViewAt(position: Int): RemoteViews =
            WidgetRenderer.scrollItem(
                ctx, WidgetRenderer.scrollText(QuoteStore.current(ctx)?.text ?: ""),
                WidgetRenderer.sizeDp(ctx, AppWidgetManager.getInstance(ctx), widgetId).first,
            )
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = WidgetRenderer.ITEM_VIEW_TYPES
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}

object WidgetRenderer {
    // Must match the padding / meta line written by tools/gen_widget_res.py.
    private const val PADDING_DP = 18f
    private const val META_DP = 18f
    private const val LINE_SPACING = 1.12f
    /** Smallest size we'll shrink to. Below this the quote scrolls instead, so it stays readable. */
    const val READABLE_SP = 13f
    private const val MAX_SP = 30f
    const val ITEM_VIEW_TYPES = WidgetStyle.ITEM_VIEW_TYPES
    /** Author line: 8dp gap + one line of ~12sp text. */
    private const val AUTHOR_DP = 26f
    /** Scroll-list item end padding (gen_widget_res.py ITEM). */
    private const val ITEM_END_DP = 8f
    /** Room around library-font text (fraction of text size) so swashes and accents aren't clipped. */
    private const val INSET_X = 0.06f
    private const val INSET_Y = 0.12f
    /** Largest quote image we'll make; about 4.5 MB as a mask. Longer quotes use a TextView. */
    private const val MAX_TEXT_PIXELS = 4_500_000L

    /** Text for the scroll-mode list: the quote with its author underneath. */
    fun scrollText(raw: String): String {
        val (body, author) = QuoteFormat.split(raw)
        return if (author != null) "$body\n\n\u2014 $author" else body
    }

    private fun component(ctx: Context) = ComponentName(ctx, QuoteWidgetProvider::class.java)

    fun hasWidgets(ctx: Context) =
        AppWidgetManager.getInstance(ctx).getAppWidgetIds(component(ctx)).isNotEmpty()

    fun updateAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        mgr.getAppWidgetIds(component(ctx)).forEach { update(ctx, mgr, it) }
    }

    /** Each widget is rendered for its own size, so two widgets of different sizes both fit. */
    /** The widget's size in dp as the launcher reports it. */
    fun sizeDp(ctx: Context, mgr: AppWidgetManager, id: Int): Pair<Int, Int> {
        val o = mgr.getAppWidgetOptions(id)
        val portrait = ctx.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        // Launchers report: portrait = minWidth x maxHeight, landscape = maxWidth x minHeight.
        val w = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val h = o.getInt(if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        return (w.takeIf { it > 0 } ?: 250) to (h.takeIf { it > 0 } ?: 110)
    }

    fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val (w, h) = sizeDp(ctx, mgr, id)
        mgr.updateAppWidget(id, build(ctx, w, h, id))
        if (Build.VERSION.SDK_INT < 31) {
            @Suppress("DEPRECATION")
            mgr.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }
    }

    /**
     * Draws [text] in a library font, because launchers can't load an app's own fonts. Most
     * styles get an alpha mask (1 byte per pixel) that the layout's android:tint colours with the
     * theme's text colour; the launcher resolves that colour, so wallpaper colours and day/night
     * switching still apply. Shadow styles get white text with the same shadow their TextViews
     * use. Returns null if the image would be unreasonably large; callers then use a TextView.
     */
    private fun textBitmap(
        ctx: Context, text: CharSequence, face: Typeface, sp: Float, widthDp: Float, gravity: Int, style: WidgetStyle,
    ): Bitmap? {
        val dm = ctx.resources.displayMetrics
        val widthPx = (widthDp * dm.density).toInt()
        val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm)
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textSize = size
            color = Color.WHITE
            if (style.shadow) setShadowLayer(8f, 0f, 1f, 0xB3000000.toInt())
        }
        val padX = (size * INSET_X).toInt()
        val padY = (size * INSET_Y).toInt()
        if (widthPx - 2 * padX <= 0) return null
        val align = if (gravity == Gravity.CENTER_HORIZONTAL) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        val layout = textLayout(text, paint, widthPx - 2 * padX, align)
        val heightPx = layout.height + 2 * padY
        if (heightPx <= 0 || widthPx.toLong() * heightPx > MAX_TEXT_PIXELS) return null
        val bmp = Bitmap.createBitmap(widthPx, heightPx, if (style.shadow) Bitmap.Config.ARGB_8888 else Bitmap.Config.ALPHA_8)
        bmp.density = dm.densityDpi
        Canvas(bmp).apply {
            translate(padX.toFloat(), padY.toFloat())
            layout.draw(this)
        }
        return bmp
    }

    private fun textLayout(text: CharSequence, paint: TextPaint, widthPx: Int, align: Layout.Alignment) =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(align)
            .setLineSpacing(0f, LINE_SPACING)
            .setIncludePad(true)
            .build()

    /**
     * The quote as one item in the scrollable list. Tapping it still goes to the next quote.
     * [widgetWidthDp] is the whole widget's width, used to draw library fonts at the right size.
     */
    fun scrollItem(ctx: Context, text: String, widgetWidthDp: Int, style: WidgetStyle = QuoteStore.style(ctx)): RemoteViews {
        val gravity = if (!style.leftAligned && QuoteStore.align(ctx) == Align.CENTER) Gravity.CENTER_HORIZONTAL else Gravity.START
        val item = RemoteViews(ctx.packageName, style.item(QuoteStore.themeMode(ctx)))
        item.setOnClickFillInIntent(R.id.widget_item, Intent())
        if (style.photo) {
            // Same text colour the photo background was rendered for.
            val color = PhotoBackground.lastColors(ctx).first
            item.setTextColor(R.id.widget_item_text, color)
            item.setInt(R.id.widget_item_img, "setColorFilter", color)
        }
        val face = QuoteStore.font(ctx).typeface(ctx)
        val bmp = face?.let {
            textBitmap(ctx, text, it, READABLE_SP, widgetWidthDp - 2 * PADDING_DP - style.startDp - ITEM_END_DP, gravity, style)
        }
        if (bmp != null) {
            item.setViewVisibility(R.id.widget_item_text, View.GONE)
            item.setImageViewBitmap(R.id.widget_item_img, bmp)
            item.setContentDescription(R.id.widget_item_img, text)
            item.setViewVisibility(R.id.widget_item_img, View.VISIBLE)
            return item
        }
        item.setTextViewText(R.id.widget_item_text, text)
        item.setTextViewTextSize(R.id.widget_item_text, TypedValue.COMPLEX_UNIT_SP, READABLE_SP)
        item.setInt(R.id.widget_item_text, "setGravity", gravity)
        return item
    }

    /**
     * Builds the widget. [style] overrides the saved style (used for the style picker), and
     * [interactive] = false builds an in-app preview: no tap actions, and long quotes are shown
     * clipped instead of in a scroll list (the list adapter can't bind inside the app).
     * [sampleText] replaces the current quote and hides the corner text (style picker tiles).
     */
    fun build(
        ctx: Context,
        widthDp: Int,
        heightDp: Int,
        widgetId: Int = 0,
        style: WidgetStyle = QuoteStore.style(ctx),
        interactive: Boolean = true,
        sampleText: String? = null,
    ): RemoteViews {
        val rv = RemoteViews(ctx.packageName, style.layout(QuoteStore.themeMode(ctx)))
        if (style.photo) {
            // The photo sized to this widget, plus text colours that read well on it.
            val dm = ctx.resources.displayMetrics
            val photo = PhotoBackground.render(ctx, (widthDp * dm.density).toInt(), (heightDp * dm.density).toInt())
            rv.setImageViewBitmap(R.id.widget_bg, photo.bitmap)
            rv.setTextColor(R.id.widget_text, photo.text)
            rv.setTextColor(R.id.widget_author, photo.accent)
            rv.setTextColor(R.id.widget_meta, photo.text)
            rv.setInt(R.id.widget_text_img, "setColorFilter", photo.text)
        }
        val openApp = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val rotation = QuoteStore.rotation(ctx)
        val q = if (sampleText != null) Quote(-1, sampleText, false) else QuoteStore.current(ctx)
        if (q == null) {
            rv.setTextViewText(R.id.widget_text, ctx.getString(R.string.widget_empty))
            rv.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, 14f)
            rv.setViewVisibility(R.id.widget_meta, View.GONE)
            if (interactive) rv.setOnClickPendingIntent(R.id.widget_body, openApp)
            return rv
        }

        val (body, author) = QuoteFormat.split(q.text)
        val pinned = rotation.any { it.pinned }
        val metaWanted = sampleText == null && (QuoteStore.showCounter(ctx) || pinned)
        val maxSp = (MAX_SP * QuoteStore.textSize(ctx).scale).coerceAtLeast(READABLE_SP)
        val font = QuoteStore.font(ctx)
        val face = font.typeface(ctx)
        val textWidthDp = widthDp - 2 * PADDING_DP - style.startDp
        val fitted = fitTextSize(
            ctx, body,
            textWidthDp,
            heightDp - 2 * PADDING_DP - (if (metaWanted) META_DP else 0f) - (if (author != null) AUTHOR_DP else 0f),
            maxSp, face ?: style.typeface(), inset = face != null,
        )
        val scrolling = fitted == null && interactive
        val hGravity = if (!style.leftAligned && QuoteStore.align(ctx) == Align.CENTER) Gravity.CENTER_HORIZONTAL else Gravity.START

        if (!scrolling) {
            val sp = fitted ?: READABLE_SP
            rv.setViewVisibility(R.id.widget_list, View.GONE)
            rv.setViewVisibility(R.id.widget_body, View.VISIBLE)
            val bmp = face?.let { textBitmap(ctx, body, it, sp, textWidthDp, hGravity, style) }
            if (bmp != null) {
                rv.setViewVisibility(R.id.widget_text, View.GONE)
                rv.setImageViewBitmap(R.id.widget_text_img, bmp)
                rv.setContentDescription(R.id.widget_text_img, body)
                rv.setViewVisibility(R.id.widget_text_img, View.VISIBLE)
            } else {
                rv.setTextViewText(R.id.widget_text, body)
                rv.setTextViewTextSize(R.id.widget_text, TypedValue.COMPLEX_UNIT_SP, sp)
                rv.setInt(R.id.widget_text, "setGravity", hGravity)
            }
            if (author != null) {
                rv.setTextViewText(R.id.widget_author, "\u2014 $author")
                rv.setTextViewTextSize(R.id.widget_author, TypedValue.COMPLEX_UNIT_SP, (sp * 0.62f).coerceIn(11f, 15f))
                rv.setInt(R.id.widget_author, "setGravity", hGravity)
                rv.setViewVisibility(R.id.widget_author, View.VISIBLE)
            } else {
                rv.setViewVisibility(R.id.widget_author, View.GONE)
            }
            if (interactive) rv.setOnClickPendingIntent(R.id.widget_body, Rotation.intent(ctx, Rotation.ACTION_NEXT))
        } else {
            // Too long to fit at a readable size: show it at READABLE_SP in a scrollable list.
            rv.setViewVisibility(R.id.widget_body, View.GONE)
            rv.setViewVisibility(R.id.widget_list, View.VISIBLE)
            if (Build.VERSION.SDK_INT >= 31) {
                rv.setRemoteAdapter(
                    R.id.widget_list,
                    RemoteViews.RemoteCollectionItems.Builder()
                        .addItem(q.id, scrollItem(ctx, scrollText(q.text), widthDp, style))
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

        val pos = Math.floorMod(QuoteStore.index(ctx), rotation.size.coerceAtLeast(1)) + 1
        val counter = when {
            sampleText != null -> null
            QuoteStore.showCounter(ctx) && pinned -> "$pos of ${rotation.size} pinned"
            QuoteStore.showCounter(ctx) -> "$pos of ${rotation.size}"
            pinned -> "Pinned"
            else -> null
        }
        val meta = listOfNotNull(if (scrolling) "Scroll for more" else null, counter).joinToString("     ")
        rv.setTextViewText(R.id.widget_meta, meta)
        // Hidden entirely when empty, so the quote gets that space.
        rv.setViewVisibility(R.id.widget_meta, if (meta.isEmpty()) View.GONE else View.VISIBLE)

        // Tap the quote = next quote. Tap the small corner text = open the app.
        if (interactive) rv.setOnClickPendingIntent(R.id.widget_meta, openApp)
        return rv
    }

    /**
     * Largest text size (sp) at which the whole quote fits in the given box, found by binary
     * search with the same line-breaking the TextView uses. Returns null if it doesn't fit even
     * at READABLE_SP (then the widget scrolls). Runs only when a widget redraws.
     */
    private fun fitTextSize(
        ctx: Context, text: String, wDp: Float, hDp: Float, maxSp: Float, face: Typeface, inset: Boolean = false,
    ): Float? {
        val dm = ctx.resources.displayMetrics
        val widthPx = (wDp * dm.density).toInt()
        // 5% safety margin: launcher fonts can differ slightly from ours.
        val heightPx = hDp * dm.density * 0.95f
        if (widthPx <= 0 || heightPx <= 0) return null
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { typeface = face }
        fun fits(sp: Float): Boolean {
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm)
            // Library fonts are drawn with a small margin (see textBitmap); measure the same way.
            val padX = if (inset) (paint.textSize * INSET_X).toInt() else 0
            val padY = if (inset) (paint.textSize * INSET_Y).toInt() else 0
            if (widthPx - 2 * padX <= 0) return false
            val layout = textLayout(text, paint, widthPx - 2 * padX, Layout.Alignment.ALIGN_NORMAL)
            return layout.height + 2 * padY <= heightPx
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
