package io.github.niravgrowlity.quotewidget

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

data class Quote(val id: Long, val text: String, val pinned: Boolean)

/** Widget look settings. */
enum class WidgetStyle(val label: String, val layout: Int) {
    TONAL("Material You (tinted)", R.layout.widget_tonal),
    SURFACE("Material You (neutral)", R.layout.widget_surface),
    ACCENT("Material You (bold accent)", R.layout.widget_accent),
    CLEAR("Transparent", R.layout.widget_clear),
}

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f), MEDIUM("Medium", 1f), LARGE("Large", 1.2f), XL("Extra large", 1.45f),
}

enum class Align(val label: String) { CENTER("Centered"), START("Left") }

/**
 * Everything lives in SharedPreferences on the phone. Nothing is uploaded anywhere.
 *
 * Rotation: the widget cycles through the "pool", which is the pinned quotes if any are pinned,
 * otherwise all quotes. `index` is the position in the (optionally shuffled) pool.
 */
object QuoteStore {
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("quotes", Context.MODE_PRIVATE)

    // ---------- quotes ----------

    fun all(ctx: Context): List<Quote> {
        val raw = prefs(ctx).getString("list", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Quote(o.getLong("id"), o.getString("text"), o.optBoolean("pinned"))
        }
    }

    private fun saveAll(ctx: Context, list: List<Quote>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text).put("pinned", it.pinned))
        }
        prefs(ctx).edit().putString("list", arr.toString()).apply()
    }

    /** Adds new quotes, skipping exact duplicates. Returns how many were added. */
    fun addAll(ctx: Context, texts: List<String>): Int = keepingCurrent(ctx) {
        val cur = all(ctx)
        val seen = cur.map { it.text }.toHashSet()
        var nextId = (cur.maxOfOrNull { it.id } ?: 0L) + 1
        val added = texts.filter { seen.add(it) }.map { Quote(nextId++, it, false) }
        saveAll(ctx, cur + added)
        added.size
    }

    fun replaceAll(ctx: Context, texts: List<String>) {
        saveAll(ctx, texts.distinct().mapIndexed { i, t -> Quote(i + 1L, t, false) })
        setIndex(ctx, 0)
    }

    fun update(ctx: Context, q: Quote) = keepingCurrent(ctx) {
        saveAll(ctx, all(ctx).map { if (it.id == q.id) q else it })
    }

    fun delete(ctx: Context, id: Long) = keepingCurrent(ctx) {
        saveAll(ctx, all(ctx).filterNot { it.id == id })
    }

    fun clearPins(ctx: Context) = keepingCurrent(ctx) {
        saveAll(ctx, all(ctx).map { it.copy(pinned = false) })
    }

    // ---------- rotation ----------

    /** The quotes the widget rotates through, in display order. */
    fun rotation(ctx: Context): List<Quote> {
        val a = all(ctx)
        val pinned = a.filter { it.pinned }
        val pool = pinned.ifEmpty { a }
        return if (shuffle(ctx)) pool.shuffled(Random(seed(ctx))) else pool
    }

    fun index(ctx: Context): Int = prefs(ctx).getInt("index", 0)
    fun setIndex(ctx: Context, i: Int) = prefs(ctx).edit().putInt("index", i).apply()

    fun current(ctx: Context): Quote? {
        val r = rotation(ctx)
        if (r.isEmpty()) return null
        return r[Math.floorMod(index(ctx), r.size)]
    }

    fun advance(ctx: Context) {
        val size = rotation(ctx).size
        if (size == 0) return
        val next = Math.floorMod(index(ctx), size) + 1
        if (next >= size) {
            // finished a full cycle; reshuffle so the next lap is in a new order
            prefs(ctx).edit().putLong("seed", Random.nextLong()).apply()
            setIndex(ctx, 0)
        } else {
            setIndex(ctx, next)
        }
    }

    /** Puts a specific quote on the widget. False if it isn't in the current rotation. */
    fun showNow(ctx: Context, id: Long): Boolean {
        val i = rotation(ctx).indexOfFirst { it.id == id }
        if (i < 0) return false
        setIndex(ctx, i)
        return true
    }

    /** Runs a change to the quote list without making the widget jump to a different quote. */
    private fun <T> keepingCurrent(ctx: Context, block: () -> T): T {
        val curId = current(ctx)?.id
        val result = block()
        if (curId != null) showNow(ctx, curId)
        return result
    }

    // ---------- settings ----------

    fun periodMinutes(ctx: Context): Long = prefs(ctx).getLong("period", 1440L)
    fun setPeriodMinutes(ctx: Context, m: Long) = prefs(ctx).edit().putLong("period", m).apply()

    fun shuffle(ctx: Context): Boolean = prefs(ctx).getBoolean("shuffle", false)
    fun setShuffle(ctx: Context, on: Boolean) = keepingCurrent(ctx) {
        prefs(ctx).edit().putBoolean("shuffle", on).apply()
    }

    private fun seed(ctx: Context): Long {
        val p = prefs(ctx)
        if (!p.contains("seed")) p.edit().putLong("seed", Random.nextLong()).apply()
        return p.getLong("seed", 0L)
    }

    fun style(ctx: Context): WidgetStyle =
        enumOr(prefs(ctx).getString("style", null), WidgetStyle.TONAL)
    fun setStyle(ctx: Context, s: WidgetStyle) = prefs(ctx).edit().putString("style", s.name).apply()

    fun textSize(ctx: Context): TextSize =
        enumOr(prefs(ctx).getString("textSize", null), TextSize.MEDIUM)
    fun setTextSize(ctx: Context, s: TextSize) = prefs(ctx).edit().putString("textSize", s.name).apply()

    fun align(ctx: Context): Align = enumOr(prefs(ctx).getString("align", null), Align.CENTER)
    fun setAlign(ctx: Context, a: Align) = prefs(ctx).edit().putString("align", a.name).apply()

    fun showCounter(ctx: Context): Boolean = prefs(ctx).getBoolean("counter", true)
    fun setShowCounter(ctx: Context, on: Boolean) = prefs(ctx).edit().putBoolean("counter", on).apply()

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}

object QuoteParser {
    private val blankLine = Regex("\\n\\s*\\n")
    private val bullet = Regex("^(?:[-*•]|\\d+[.)])\\s+")

    fun hasBlankLines(text: String) = blankLine.containsMatchIn(text.replace("\r\n", "\n").trim())

    fun parse(text: String, blankLineSeparated: Boolean): List<String> {
        val norm = text.replace("\r\n", "\n")
        val parts = if (blankLineSeparated) norm.split(blankLine) else norm.split('\n')
        return parts
            .map { bullet.replaceFirst(it.trim(), "").trim() }
            .filter { it.isNotEmpty() }
    }
}
