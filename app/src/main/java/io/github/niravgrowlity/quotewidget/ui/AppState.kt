package io.github.niravgrowlity.quotewidget.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.ThemeMode
import io.github.niravgrowlity.quotewidget.Updater
import io.github.niravgrowlity.quotewidget.WidgetRenderer

/** A snackbar message, optionally with an action such as Undo. */
class Msg(val text: String, val action: String? = null, val onAction: () -> Unit = {})

/** Implemented by MainActivity, which owns the install flow. */
interface UpdateActions {
    fun checkForUpdate(manual: Boolean)
    fun install(release: Updater.Release)
    fun openInstallSettings(release: Updater.Release)
}

/**
 * UI state. Quotes and settings live in QuoteStore; screens read it directly and observe
 * [tick], which bumps after every change so they recompose.
 *
 * One instance per process (see [get]), so a download in progress, an open dialog or a pending
 * update survive the activity being recreated (rotation, dark-mode switch).
 */
class AppState private constructor(private val ctx: Context) {
    /** The visible activity; swapped on recreation. */
    lateinit var updates: UpdateActions

    /** An update waiting for the user to allow "install unknown apps". */
    var pendingRelease: Updater.Release? = null

    companion object {
        @Volatile private var instance: AppState? = null
        fun get(ctx: Context): AppState =
            instance ?: synchronized(this) { instance ?: AppState(ctx.applicationContext).also { instance = it } }

        /** The application context once the app has started, for the crash handler. */
        val appContext: Context? get() = instance?.ctx
    }

    var tick by mutableIntStateOf(0)
        private set
    var themeMode by mutableStateOf(QuoteStore.themeMode(ctx))
        private set

    /** The collection open on the Quotes tab; Add quotes adds to it by default. */
    var collection by mutableLongStateOf(QuoteStore.collections(ctx).first().id)

    var message by mutableStateOf<Msg?>(null)
    var available by mutableStateOf<Updater.Release?>(null)
    var needsInstallPermission by mutableStateOf<Updater.Release?>(null)
    var updateStatus by mutableStateOf<String?>(null)
    var updating by mutableStateOf(false)

    /** Call after changing anything; also redraws home-screen widgets unless told not to. */
    fun changed(widget: Boolean = true) {
        tick++
        if (widget) WidgetRenderer.updateAll(ctx)
    }

    /** Re-reads settings that the UI keeps a copy of (after a restore). */
    fun reload() {
        themeMode = QuoteStore.themeMode(ctx)
        collection = QuoteStore.collections(ctx).first().id
        changed()
    }

    fun setTheme(mode: ThemeMode) {
        QuoteStore.setThemeMode(ctx, mode)
        themeMode = mode
        changed()
    }

    fun say(text: String, action: String? = null, onAction: () -> Unit = {}) {
        message = Msg(text, action, onAction)
    }
}
