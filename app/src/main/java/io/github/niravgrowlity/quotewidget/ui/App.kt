package io.github.niravgrowlity.quotewidget.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.QuoteStore

@Composable
fun QuoteApp(state: AppState) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val m = state.message ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(m.text, actionLabel = m.action, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) m.onAction()
        if (state.message === m) state.message = null
    }

    if (adding) {
        BackHandler { adding = false }
        AddQuotesScreen(state, onClose = { adding = false })
    } else {
        state.tick
        val hasQuotes = QuoteStore.all(ctx).isNotEmpty()
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(AppIcons.Quote, contentDescription = null) },
                        label = { Text("Quotes") },
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(AppIcons.Widgets, contentDescription = null) },
                        label = { Text("Widget") },
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                    )
                }
            },
            floatingActionButton = {
                if (tab == 0 && hasQuotes) {
                    ExtendedFloatingActionButton(
                        onClick = { adding = true },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text("Add quotes") },
                    )
                }
            },
        ) { padding ->
            when (tab) {
                0 -> QuotesScreen(state, padding, onAdd = { adding = true })
                1 -> WidgetScreen(state, padding)
                else -> SettingsScreen(state, padding)
            }
        }
    }

    UpdateDialogs(state)
}

@Composable
private fun UpdateDialogs(state: AppState) {
    state.available?.let { release ->
        AlertDialog(
            onDismissRequest = { state.available = null },
            title = { Text("Update to ${release.name}") },
            text = {
                Text(
                    release.notes.ifBlank { "A new version is ready to install." },
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
            text = {
                Text(
                    "Android asks once before an app can install its own updates. " +
                        "Turn on “Allow from this source”, then come back. The update continues by itself.",
                )
            },
            confirmButton = {
                TextButton(onClick = { state.updates.openInstallSettings(release) }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { state.needsInstallPermission = null }) { Text("Cancel") } },
        )
    }
}
