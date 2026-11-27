package io.github.niravgrowlity.quotewidget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.QuoteStore

/** The stage is always underneath; the others are pages that slide up over it. */
private enum class Dest { Stage, Quotes, Widget, Settings, Add }

@Composable
fun QuoteApp(state: AppState) {
    val ctx = LocalContext.current
    var dest by rememberSaveable { mutableStateOf(Dest.Stage) }
    var addFromStage by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(m.text, actionLabel = m.action, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) m.onAction()
        if (state.message === m) state.message = null
    }

    val close = { dest = Dest.Stage }
    val closeAdd = { dest = if (addFromStage) Dest.Stage else Dest.Quotes }
    BackHandler(enabled = dest != Dest.Stage) { if (dest == Dest.Add) closeAdd() else close() }

    Box(Modifier.fillMaxSize()) {
        StageScreen(
            state = state,
            covered = dest != Dest.Stage,
            onQuotes = { dest = Dest.Quotes },
            onWidget = { dest = Dest.Widget },
            onSettings = { dest = Dest.Settings },
            onAdd = { addFromStage = true; dest = Dest.Add },
        )

        AnimatedContent(
            targetState = dest,
            transitionSpec = {
                when {
                    // Opening a page: it rises over the stage, which stays put.
                    initialState == Dest.Stage ->
                        slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f, visibilityThreshold = IntOffset(1, 1))) { it } togetherWith
                            ExitTransition.None
                    // Closing: it drops away.
                    targetState == Dest.Stage ->
                        EnterTransition.None togetherWith slideOutVertically(tween(240, easing = FastOutLinearInEasing)) { it }
                    else ->
                        (fadeIn(tween(200)) + slideInHorizontally(tween(260)) { it / 10 }) togetherWith fadeOut(tween(120))
                }
            },
            label = "page",
        ) { d ->
            when (d) {
                Dest.Stage -> Spacer(Modifier.fillMaxSize())
                Dest.Quotes -> {
                    state.tick
                    val hasQuotes = QuoteStore.all(ctx).isNotEmpty()
                    Page(
                        "Quotes", close,
                        fab = {
                            if (hasQuotes) {
                                ExtendedFloatingActionButton(
                                    onClick = { addFromStage = false; dest = Dest.Add },
                                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    text = { Text("Add quotes") },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        },
                    ) { padding -> QuotesScreen(state, padding, onAdd = { addFromStage = false; dest = Dest.Add }) }
                }
                Dest.Widget -> Page("Widget", close) { padding -> WidgetScreen(state, padding) }
                Dest.Settings -> Page("Settings", close) { padding -> SettingsScreen(state, padding) }
                Dest.Add -> AddQuotesScreen(state, onClose = closeAdd)
            }
        }

        // One host for every page, clear of the stage controls and the Add quotes button.
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(bottom = 84.dp),
        )
    }

    UpdateDialogs(state)
}

/** A page over the stage: its name, a way back down, and the screen itself. */
@Composable
private fun Page(
    title: String,
    onClose: () -> Unit,
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                PopButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close")
                }
            }
        },
        floatingActionButton = fab,
        content = content,
    )
}

@Composable
private fun UpdateDialogs(state: AppState) {
    state.available?.let { release ->
        AlertDialog(
            onDismissRequest = { state.available = null },
            title = { Text("Update to ${release.name}") },
            text = {
                Text(
                    release.notes.ifBlank { "A new version is ready." },
                    modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { state.updates.install(release) }) { Text("Install") } },
            dismissButton = { TextButton(onClick = { state.available = null }) { Text("Later") } },
        )
    }
    state.needsInstallPermission?.let { release ->
        AlertDialog(
            onDismissRequest = { state.needsInstallPermission = null },
            title = { Text("Allow updates") },
            text = { Text("Turn on “Allow from this source”, then come back.") },
            confirmButton = {
                TextButton(onClick = { state.updates.openInstallSettings(release) }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { state.needsInstallPermission = null }) { Text("Cancel") } },
        )
    }
}
