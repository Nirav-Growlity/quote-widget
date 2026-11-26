package io.github.niravgrowlity.quotewidget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app updater. Every push to the GitHub repo builds a release tagged v1.0.<build> with the
 * APK attached. We compare <build> with our own versionCode and install newer ones in place,
 * so updates never need a browser or file manager.
 */
object Updater {
    data class Release(val versionCode: Int, val name: String, val apkUrl: String, val notes: String)

    /**
     * The system's "Update this app?" screen, waiting to be shown. It's launched by MainActivity
     * (while visible) rather than from the background, which newer Android versions block.
     */
    @Volatile var pendingConfirm: Intent? = null
    var onConfirmReady: (() -> Unit)? = null

    /** Network call; run it off the main thread. */
    fun fetchLatest(): Release? {
        val conn = URL("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        try {
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            val code = tag.substringAfterLast('.').toIntOrNull() ?: return null
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) {
                    return Release(code, tag, a.getString("browser_download_url"), json.optString("body"))
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK (reporting 0-100 progress), checks it, and hands it to Android's installer. */
    fun downloadAndInstall(ctx: Context, release: Release, progress: (Int) -> Unit) {
        val file = File(ctx.cacheDir, "update.apk")
        val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        try {
            if (conn.responseCode != 200) error("download failed (HTTP ${conn.responseCode})")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) { lastPct = pct; progress(pct) }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        // Sanity check before installing: it must be this app, and newer than what's installed.
        @Suppress("DEPRECATION")
        val info = ctx.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: error("the downloaded file is damaged; try again")
        if (info.packageName != ctx.packageName) error("the downloaded file isn't Quote Widget")
        @Suppress("DEPRECATION")
        val newCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        if (newCode <= BuildConfig.VERSION_CODE) error("already up to date")

        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= 31) {
            // Once this app has installed itself once, Android 12+ can update it without the
            // extra confirmation screen. If it isn't allowed, Android just asks as usual.
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("update.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val callback = PendingIntent.getBroadcast(
                ctx, sessionId, Intent(ctx, InstallReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }
}

/** Receives installer status from Android. */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                Updater.pendingConfirm = intent.getParcelableExtra(Intent.EXTRA_INTENT)
                // If the app is on screen, show the confirm screen now; otherwise MainActivity
                // shows it the next time it's opened.
                Updater.onConfirmReady?.invoke()
            }
            PackageInstaller.STATUS_SUCCESS -> Unit // Android restarts the app on the new version
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "code $status"
                Toast.makeText(ctx, "Update failed: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }
}
