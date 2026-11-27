package io.github.niravgrowlity.quotewidget.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.niravgrowlity.quotewidget.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.niravgrowlity.quotewidget.BuildConfig
import io.github.niravgrowlity.quotewidget.QuoteStore
import io.github.niravgrowlity.quotewidget.ThemeMode

/**
 * App-wide settings: theme, updates and about. Widget looks stay in the Widget tab.
 * New app-level options belong here.
 */
@Composable
fun SettingsScreen(state: AppState, padding: PaddingValues) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    state.tick
    val repo = "https://github.com/${BuildConfig.UPDATE_REPO}"
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf<Backup.Contents?>(null) }

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.export(ctx).toByteArray()) }
                    }.isSuccess
                }
                state.say(if (ok) "Backup saved" else "Couldn't save the backup")
            }
        }
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        Backup.parse(text)
                    }
                }
                result.onSuccess { restoring = it }
                    .onFailure { state.say((it as? IllegalArgumentException)?.message ?: "Couldn't read that file") }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        Setting("Theme") {
            Segmented(ThemeMode.entries, state.themeMode, { it.label }) { state.setTheme(it) }
        }

        SectionTitle("Updates")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
                state.updateStatus?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton(onClick = { state.updates.checkForUpdate(manual = true) }, enabled = !state.updating) {
                Text("Check")
            }
        }
        SwitchRow("Check when the app opens", null, QuoteStore.autoUpdateCheck(ctx)) {
            QuoteStore.setAutoUpdateCheck(ctx, it)
            state.changed(widget = false)
        }

        SectionTitle("Backup")
        LinkRow("Back up", "Quotes, pins and widget settings. Not the photo.") {
            saveBackup.launch("quotes-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json")
        }
        LinkRow("Restore") {
            openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }

        SectionTitle("About")
        LinkRow("Source code", "github.com/${BuildConfig.UPDATE_REPO}") { uri.openUri(repo) }
        LinkRow("Font licences") { uri.openUri("$repo/blob/main/FONT_LICENSES.txt") }
        LinkRow("Theme credits") { uri.openUri("$repo#credits") }
    }

    restoring?.let { RestoreDialog(state, it) { restoring = null } }
}

@Composable
private fun RestoreDialog(state: AppState, backup: Backup.Contents, onDone: () -> Unit) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Restore backup?") },
        text = {
            Text(
                "It has ${backup.summary}. Add them to your quotes, or replace everything you have now.",
            )
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = {
                    val added = Backup.merge(ctx, backup)
                    state.changed()
                    state.say(if (added == 1) "Added 1 quote" else "Added $added quotes")
                    onDone()
                }) { Text("Add to mine") }
                TextButton(onClick = {
                    val before = QuoteStore.snapshot(ctx)
                    Backup.replace(ctx, backup)
                    state.reload()
                    state.say("Restored ${backup.summary}", "Undo") {
                        QuoteStore.restoreSnapshot(ctx, before)
                        state.reload()
                    }
                    onDone()
                }) { Text("Replace everything", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDone) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun LinkRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
