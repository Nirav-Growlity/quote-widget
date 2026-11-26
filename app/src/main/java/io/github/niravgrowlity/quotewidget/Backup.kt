package io.github.niravgrowlity.quotewidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Backup file: plain JSON with the collections, the quotes (in your order, with pins) and the
 * widget/rotation settings. The photo background isn't included.
 */
object Backup {
    private const val FORMAT = 1

    fun export(ctx: Context): String {
        val cols = JSONArray()
        QuoteStore.collections(ctx).forEach { cols.put(JSONObject().put("id", it.id).put("name", it.name)) }
        val quotes = JSONArray()
        QuoteStore.all(ctx).forEach {
            quotes.put(JSONObject().put("id", it.id).put("text", it.text).put("pinned", it.pinned).put("collection", it.collection))
        }
        return JSONObject()
            .put("app", "quote-widget")
            .put("format", FORMAT)
            .put("exported", System.currentTimeMillis())
            .put("collections", cols)
            .put("quotes", quotes)
            .put("settings", QuoteStore.exportSettings(ctx))
            .toString(2)
    }

    class Contents(val collections: List<QuoteCollection>, val quotes: List<Quote>, val settings: JSONObject?) {
        val summary: String
            get() {
                val q = if (quotes.size == 1) "1 quote" else "${quotes.size} quotes"
                val c = if (collections.size == 1) "1 collection" else "${collections.size} collections"
                return "$q in $c"
            }
    }

    /**
     * Reads and checks a whole backup before anything is changed. Also accepts a plain text file
     * (one quote per line) as a single collection. Throws IllegalArgumentException with a
     * readable message if the file isn't usable.
     */
    fun parse(text: String): Contents {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) {
            val lines = QuoteParser.parse(trimmed, QuoteParser.hasBlankLines(trimmed))
            require(lines.isNotEmpty()) { "That file has no quotes in it." }
            val c = QuoteCollection(QuoteStore.DEFAULT_COLLECTION, "Imported")
            return Contents(listOf(c), lines.mapIndexed { i, t -> Quote(i + 1L, t, false, c.id) }, null)
        }
        val root = try { JSONObject(trimmed) } catch (e: Exception) { throw IllegalArgumentException("That file isn't a Quote Widget backup.") }
        require(root.optString("app") == "quote-widget") { "That file isn't a Quote Widget backup." }
        require(root.optInt("format", 0) <= FORMAT) { "This backup is from a newer version. Update the app first." }

        val colsArr = root.optJSONArray("collections") ?: JSONArray()
        val cols = (0 until colsArr.length()).map {
            val o = colsArr.getJSONObject(it)
            QuoteCollection(o.getLong("id"), o.optString("name").ifBlank { "Collection" })
        }.distinctBy { it.id }.ifEmpty { listOf(QuoteCollection(QuoteStore.DEFAULT_COLLECTION, "My quotes")) }
        val ids = cols.map { it.id }.toSet()

        val qArr = root.optJSONArray("quotes") ?: throw IllegalArgumentException("That backup has no quotes in it.")
        val quotes = (0 until qArr.length()).mapNotNull {
            val o = qArr.getJSONObject(it)
            val t = o.optString("text").trim()
            if (t.isEmpty()) return@mapNotNull null
            val c = o.optLong("collection", cols.first().id).takeIf { id -> id in ids } ?: cols.first().id
            Quote(o.optLong("id", it + 1L), t, o.optBoolean("pinned"), c)
        }.distinctBy { it.id }
        return Contents(cols, quotes, root.optJSONObject("settings"))
    }

    /** Saved automatically before a "replace" restore, so nothing is ever lost by accident. */
    private fun safetyFile(ctx: Context) = File(ctx.filesDir, "before-restore.json")

    /** Replaces all collections, quotes and settings with the backup's. */
    fun replace(ctx: Context, b: Contents) {
        safetyFile(ctx).writeText(export(ctx))
        QuoteStore.replaceEverything(ctx, b.collections, b.quotes)
        b.settings?.let { QuoteStore.importSettings(ctx, it) }
    }

    /**
     * Adds the backup to what's already here. Collections are matched by name; quotes already in
     * that collection are skipped. Returns how many quotes were added.
     */
    fun merge(ctx: Context, b: Contents): Int {
        val lastIdBefore = QuoteStore.all(ctx).maxOfOrNull { it.id } ?: 0L
        val byName = QuoteStore.collections(ctx).associateBy { it.name.trim().lowercase() }.toMutableMap()
        var added = 0
        b.collections.forEach { bc ->
            val target = byName[bc.name.trim().lowercase()]
                ?: QuoteStore.addCollection(ctx, bc.name).also { byName[it.name.trim().lowercase()] = it }
            val texts = b.quotes.filter { it.collection == bc.id }.map { it.text }
            added += QuoteStore.addAll(ctx, texts, target.id)
        }
        // Keep pins from the backup on the quotes it added.
        val pinned = b.quotes.filter { it.pinned }.map { it.text }.toSet()
        if (pinned.isNotEmpty()) {
            QuoteStore.all(ctx).filter { it.id > lastIdBefore && it.text in pinned }.forEach {
                QuoteStore.update(ctx, it.copy(pinned = true))
            }
        }
        return added
    }
}
