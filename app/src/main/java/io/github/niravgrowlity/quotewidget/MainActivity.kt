package io.github.niravgrowlity.quotewidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import io.github.niravgrowlity.quotewidget.ui.AppState
import io.github.niravgrowlity.quotewidget.ui.QuoteApp
import io.github.niravgrowlity.quotewidget.ui.QuoteTheme
import io.github.niravgrowlity.quotewidget.ui.UpdateActions
import io.github.niravgrowlity.quotewidget.ui.isDark
import kotlin.concurrent.thread

class MainActivity : ComponentActivity(), UpdateActions {
    private lateinit var state: AppState
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        state = AppState.get(this)
        state.updates = this

        setContent {
            val dark = isDark(state.themeMode)
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            QuoteTheme(dark) { QuoteApp(state) }
        }

        Rotation.schedule(this, force = false)
    }

    /**
     * Each time the app is opened it checks for a newer release and, if there is one, shows the
     * update popup. Network is only used here, in the foreground. After a crash it checks
     * regardless of the 10-minute limit, so a broken release can always be fixed by an update.
     */
    override fun onStart() {
        super.onStart()
        val crashed = QuoteStore.takeCrashFlag(this)
        if (state.updating || state.available != null) return
        if (crashed || QuoteStore.updateCheckDue(this)) checkForUpdate(manual = false)
    }

    companion object {
        init {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                runCatching { QuoteStore.markCrashed(AppState.appContext) }
                previous?.uncaughtException(t, e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The widget may have moved on (taps, timer) while we were away.
        state.changed(widget = false)
        // Returning from the installer's confirm screen without installing.
        if (state.updateStatus?.startsWith("Installing") == true) {
            state.updateStatus = null
            state.updating = false
        }
        Updater.onConfirmReady = { showPendingConfirm() }
        showPendingConfirm()
        val release = state.pendingRelease
        if (release != null && packageManager.canRequestPackageInstalls()) {
            state.pendingRelease = null
            install(release)
        }
    }

    override fun onPause() {
        Updater.onConfirmReady = null
        super.onPause()
    }

    private fun showPendingConfirm() {
        val confirm = Updater.pendingConfirm ?: return
        Updater.pendingConfirm = null
        try {
            startActivity(confirm)
        } catch (e: Exception) {
            state.say("Couldn't open the installer: ${e.message}")
        }
    }

    override fun checkForUpdate(manual: Boolean) {
        if (manual) state.updateStatus = "Checking for updates…"
        thread {
            val release = try { Updater.fetchLatest() } catch (_: Exception) { null }
            main.post {
                if (manual) state.updateStatus = null
                when {
                    release == null -> if (manual) state.say("Couldn't reach GitHub. Check your connection and try again.")
                    release.versionCode <= BuildConfig.VERSION_CODE -> if (manual) state.say("You’re on the latest version")
                    else -> state.available = release
                }
            }
        }
    }

    override fun install(release: Updater.Release) {
        state.available = null
        if (state.updating) return
        if (!packageManager.canRequestPackageInstalls()) {
            state.needsInstallPermission = release
            return
        }
        state.updating = true
        state.updateStatus = "Downloading ${release.name}…"
        thread {
            try {
                Updater.downloadAndInstall(this, release) { pct ->
                    main.post { state.updateStatus = "Downloading ${release.name}, $pct%" }
                }
                main.post { state.updateStatus = "Installing ${release.name}…" }
            } catch (e: Exception) {
                main.post {
                    state.updating = false
                    state.updateStatus = null
                    state.say("Update failed: ${e.message}")
                }
            }
        }
    }

    override fun openInstallSettings(release: Updater.Release) {
        state.needsInstallPermission = null
        state.pendingRelease = release
        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
    }
}
