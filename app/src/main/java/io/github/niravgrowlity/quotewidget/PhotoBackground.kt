package io.github.niravgrowlity.quotewidget

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.palette.graphics.Palette
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** How the photo is sized to the widget's shape. */
enum class PhotoFit(val label: String) { FILL("Fill"), FIT("Fit") }

/**
 * The "Your photo" widget background. The picked image is stored once, downscaled, in the app's
 * private files. Each widget redraw sizes it to that widget (cropped to fill, or fitted with the
 * photo's own colour around it), then applies blur, a colour overlay and, if needed, an extra
 * scrim so the text always has enough contrast. The text itself is drawn on top as real text,
 * so it stays sharp.
 */
object PhotoBackground {
    private const val FILE = "widget_photo.jpg"
    private const val MAX_STORED = 1600
    /** Widget backgrounds are rendered at most this many pixels on the long side. */
    private const val MAX_RENDER = 1000
    /** WCAG AA contrast for normal text; a busy photo gets a higher target. */
    private const val MIN_CONTRAST = 4.5
    private const val BUSY_CONTRAST = 6.0
    private const val DARK_TEXT = 0xFF1C1B1F.toInt()
    private const val LIGHT_TEXT = 0xFFFFFFFF.toInt()

    class Rendered(val bitmap: Bitmap, val text: Int, val accent: Int)

    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences("photo", Context.MODE_PRIVATE)
    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    fun has(ctx: Context) = file(ctx).exists()

    // ---------- settings ----------

    fun fit(ctx: Context): PhotoFit =
        PhotoFit.entries.firstOrNull { it.name == prefs(ctx).getString("fit", null) } ?: PhotoFit.FILL
    fun setFit(ctx: Context, f: PhotoFit) = edit(ctx) { putString("fit", f.name) }

    /** Blur strength, 0 (off) to 20. */
    fun blur(ctx: Context): Int = prefs(ctx).getInt("blur", 0)
    fun setBlur(ctx: Context, v: Int) = edit(ctx) { putInt("blur", v.coerceIn(0, 20)) }

    /** Overlay strength in percent, 0 to 80. */
    fun overlay(ctx: Context): Int = prefs(ctx).getInt("overlay", 20)
    fun setOverlay(ctx: Context, v: Int) = edit(ctx) { putInt("overlay", v.coerceIn(0, 80)) }

    fun overlayColor(ctx: Context): Int = prefs(ctx).getInt("overlayColor", 0xFF000000.toInt())
    fun setOverlayColor(ctx: Context, c: Int) = edit(ctx) { putInt("overlayColor", c) }

    /** Null = pick white or dark automatically from the photo. */
    fun textColor(ctx: Context): Int? = prefs(ctx).let { if (it.contains("textColor")) it.getInt("textColor", LIGHT_TEXT) else null }
    fun setTextColor(ctx: Context, c: Int?) = edit(ctx) { if (c == null) remove("textColor") else putInt("textColor", c) }

    /** Adds extra darkening/lightening when needed so the text stays readable. */
    fun keepReadable(ctx: Context): Boolean = prefs(ctx).getBoolean("readable", true)
    fun setKeepReadable(ctx: Context, on: Boolean) = edit(ctx) { putBoolean("readable", on) }

    /** Colours taken from the photo, most prominent first. */
    fun swatches(ctx: Context): List<Int> =
        prefs(ctx).getString("swatches", null)?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList()

    /** Text and accent colours from the last render, for list items drawn separately. */
    fun lastColors(ctx: Context): Pair<Int, Int> =
        prefs(ctx).getInt("lastText", LIGHT_TEXT) to prefs(ctx).getInt("lastAccent", LIGHT_TEXT)

    private inline fun edit(ctx: Context, block: SharedPreferences.Editor.() -> Unit) {
        prefs(ctx).edit().apply(block).apply()
        invalidate()
    }

    // ---------- import ----------

    /** Copies a picked image into the app (downscaled) and reads its colours. Call off the main thread. */
    fun import(ctx: Context, uri: Uri): Boolean {
        val bmp = runCatching { decode(ctx, uri) }.getOrNull() ?: return false
        val tmp = File(ctx.filesDir, "$FILE.tmp")
        tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        if (!tmp.renameTo(file(ctx))) {
            file(ctx).delete()
            if (!tmp.renameTo(file(ctx))) return false
        }
        val palette = Palette.from(bmp).maximumColorCount(16).generate()
        val colors = listOfNotNull(
            palette.dominantSwatch, palette.vibrantSwatch, palette.darkVibrantSwatch, palette.lightVibrantSwatch,
            palette.mutedSwatch, palette.darkMutedSwatch, palette.lightMutedSwatch,
        ).map { it.rgb or 0xFF000000.toInt() }.distinct().take(6)
        prefs(ctx).edit()
            .putString("swatches", colors.joinToString(","))
            .putLong("version", System.currentTimeMillis())
            .remove("textColor")
            .apply()
        invalidate()
        source = null
        return true
    }

    fun remove(ctx: Context) {
        file(ctx).delete()
        prefs(ctx).edit().remove("swatches").putLong("version", System.currentTimeMillis()).apply()
        invalidate()
        source = null
    }

    private fun decode(ctx: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder applies the photo's rotation (EXIF) for us.
            val src = ImageDecoder.createSource(ctx.contentResolver, uri)
            return ImageDecoder.decodeBitmap(src) { dec, info, _ ->
                val s = info.size
                val scale = min(1f, MAX_STORED.toFloat() / max(s.width, s.height))
                dec.setTargetSize((s.width * scale).roundToInt().coerceAtLeast(1), (s.height * scale).roundToInt().coerceAtLeast(1))
                dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_STORED) sample *= 2
        var bmp = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = cr.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = min(1f, MAX_STORED.toFloat() / max(bmp.width, bmp.height))
        if (rotation != 0f || scale < 1f) {
            val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        return bmp
    }

    // ---------- render ----------

    @Volatile private var source: Pair<Long, Bitmap>? = null
    /** Last few renders by size and settings; each widget can have its own size. */
    private val renders = object : LinkedHashMap<String, Rendered>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Rendered>?) = size > 4
    }
    private fun invalidate() = synchronized(renders) { renders.clear() }

    private fun sourceBitmap(ctx: Context): Bitmap? {
        val version = prefs(ctx).getLong("version", 0L)
        source?.let { if (it.first == version) return it.second }
        val f = file(ctx)
        if (!f.exists()) return null
        val bmp = BitmapFactory.decodeFile(f.path) ?: return null
        source = version to bmp
        return bmp
    }

    /**
     * The background for a widget of [wPx] x [hPx] pixels with rounded corners, plus the text and
     * accent colours to draw on it. Without a photo, a neutral placeholder.
     */
    fun render(ctx: Context, wPx: Int, hPx: Int): Rendered {
        val scaleDown = min(1f, MAX_RENDER.toFloat() / max(wPx, hPx))
        val w = (wPx * scaleDown).roundToInt().coerceAtLeast(8)
        val h = (hPx * scaleDown).roundToInt().coerceAtLeast(8)
        val p = prefs(ctx)
        val key = "$w:$h:${p.getLong("version", 0L)}:${p.all.hashCode()}"
        synchronized(renders) { renders[key] }?.let { return it }

        val radius = ctx.resources.getDimension(R.dimen.widget_radius) * scaleDown
        val src = sourceBitmap(ctx)
        val swatches = swatches(ctx)
        val base = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(base)
        if (src == null) {
            c.drawColor(0xFF3A3A40.toInt())
        } else {
            val fill = fit(ctx) == PhotoFit.FILL
            if (!fill) c.drawColor(swatches.firstOrNull() ?: 0xFF202024.toInt())
            val s = if (fill) max(w.toFloat() / src.width, h.toFloat() / src.height)
                    else min(w.toFloat() / src.width, h.toFloat() / src.height)
            val m = Matrix().apply {
                postScale(s, s)
                postTranslate((w - src.width * s) / 2f, (h - src.height * s) / 2f)
            }
            c.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        val blurred = if (src != null && blur(ctx) > 0) blur(base, blur(ctx)) else base
        val cb = Canvas(blurred)

        // User's overlay colour.
        val overlay = overlayColor(ctx)
        val overlayAlpha = overlay(ctx) / 100f
        if (overlayAlpha > 0f) cb.drawColor(withAlpha(overlay, overlayAlpha))

        // Pick the text colour and, if asked, add just enough scrim for readable contrast.
        // Light text is checked against the brightest parts of the photo and dark text against
        // the darkest, so a bright patch behind the words can't make them unreadable.
        val stats = sample(blurred)
        val text = textColor(ctx) ?: if (luminance(stats.average) > 0.4) DARK_TEXT else LIGHT_TEXT
        if (keepReadable(ctx)) {
            val lightText = luminance(text) > 0.5
            val worst = if (lightText) stats.brightest else stats.darkest
            val target = if (stats.busy > 0.18 && blur(ctx) < 6) BUSY_CONTRAST else MIN_CONTRAST
            val scrim = if (lightText) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            if (contrast(text, worst) < target) {
                var lo = 0f
                var hi = 0.85f
                repeat(12) {
                    val mid = (lo + hi) / 2
                    if (contrast(text, mix(worst, scrim, mid)) >= target) hi = mid else lo = mid
                }
                cb.drawColor(withAlpha(scrim, hi))
            }
        }
        val finalAvg = sample(blurred).average
        val accent = (swatches + text).firstOrNull { contrast(it, finalAvg) >= 3.0 } ?: text

        // Rounded corners, so the photo matches the widget's shape on every Android version.
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawRoundRect(
            RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) },
        )
        p.edit().putInt("lastText", text).putInt("lastAccent", accent).apply()
        return Rendered(out, text, accent).also { synchronized(renders) { renders[key] = it } }
    }

    /** Soft blur: shrink, box-blur three times (close to a Gaussian), and scale back up. */
    private fun blur(src: Bitmap, strength: Int): Bitmap {
        val factor = 4
        val sw = (src.width / factor).coerceAtLeast(1)
        val sh = (src.height / factor).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        val r = (strength / 2).coerceAtLeast(1)
        repeat(3) {
            boxBlur(px, sw, sh, r, horizontal = true)
            boxBlur(px, sw, sh, r, horizontal = false)
        }
        small.setPixels(px, 0, sw, 0, 0, sw, sh)
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            small, null, RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        return out
    }

    private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val len = if (horizontal) w else h
        val lines = if (horizontal) h else w
        val line = IntArray(len)
        for (l in 0 until lines) {
            for (i in 0 until len) line[i] = px[if (horizontal) l * w + i else i * w + l]
            var sr = 0; var sg = 0; var sb = 0
            val div = 2 * r + 1
            for (i in -r..r) {
                val c = line[i.coerceIn(0, len - 1)]
                sr += (c shr 16) and 255; sg += (c shr 8) and 255; sb += c and 255
            }
            for (i in 0 until len) {
                px[if (horizontal) l * w + i else i * w + l] =
                    (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val out = line[(i - r).coerceIn(0, len - 1)]
                val inn = line[(i + r + 1).coerceIn(0, len - 1)]
                sr += ((inn shr 16) and 255) - ((out shr 16) and 255)
                sg += ((inn shr 8) and 255) - ((out shr 8) and 255)
                sb += (inn and 255) - (out and 255)
            }
        }
    }

    private class Stats(val average: Int, val brightest: Int, val darkest: Int, val busy: Double)

    /**
     * Colour statistics of the area behind the text (the middle of the widget): the average,
     * a near-brightest and near-darkest colour (90th/10th percentile, so one stray pixel doesn't
     * count), and how busy it is (spread of brightness, 0..0.5).
     */
    private fun sample(bmp: Bitmap): Stats {
        val inner = Bitmap.createBitmap(bmp, bmp.width / 10, bmp.height / 8, bmp.width * 8 / 10, bmp.height * 6 / 8)
        val px = IntArray(24 * 24)
        Bitmap.createScaledBitmap(inner, 24, 24, true).getPixels(px, 0, 24, 0, 0, 24, 24)
        val byLum = px.sortedBy { luminance(it) }
        val (avg, busy) = averageAndBusyness(px)
        return Stats(avg, byLum[byLum.size * 9 / 10], byLum[byLum.size / 10], busy)
    }

    private fun averageAndBusyness(px: IntArray): Pair<Int, Double> {
        var r = 0L; var g = 0L; var b = 0L
        var sum = 0.0; var sq = 0.0
        for (c in px) {
            r += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
            val l = luminance(c)
            sum += l; sq += l * l
        }
        val n = px.size
        val mean = sum / n
        val avg = (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
        return avg to sqrt((sq / n - mean * mean).coerceAtLeast(0.0))
    }

    private fun withAlpha(color: Int, a: Float) = ((a * 255).roundToInt().coerceIn(0, 255) shl 24) or (color and 0xFFFFFF)

    private fun mix(base: Int, over: Int, a: Float): Int {
        fun ch(shift: Int) = (((base shr shift) and 255) * (1 - a) + ((over shr shift) and 255) * a).roundToInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** WCAG relative luminance. */
    fun luminance(c: Int): Double {
        fun lin(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin((c shr 16) and 255) + 0.7152 * lin((c shr 8) and 255) + 0.0722 * lin(c and 255)
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a); val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
