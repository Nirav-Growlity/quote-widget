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
            .padding(top = padding.calculateTopPadding() + 20.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
        )

        SectionTitle("Appearance")
        Setting("Theme") {
            Segmented(ThemeMode.entries, state.themeMode, { it.label }) { state.setTheme(it) }
            Hint("Applies to the app and the “Match my wallpaper” widget styles. Palette styles keep their own colours.")
        }

        SectionTitle("Updates")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    state.updateStatus ?: "New versions install from inside the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = { state.updates.checkForUpdate(manual = true) }, enabled = !state.updating) {
                Text("Check now")
            }
        }
        SwitchRow(
            "Check when the app opens",
            "Shows a popup when a new version is out. Uses the network only while the app is open.",
            QuoteStore.autoUpdateCheck(ctx),
        ) {
            QuoteStore.setAutoUpdateCheck(ctx, it)
            state.changed(widget = false)
        }

        SectionTitle("Backup")
        LinkRow("Back up quotes", "Saves your collections, quotes, pins and widget settings to a file") {
            saveBackup.launch("quotes-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json")
        }
        LinkRow("Restore from a backup", "Add a backup to your quotes, or replace everything with it") {
            openBackup.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        }
        Text(
            "Keep the file somewhere safe, like Google Drive. The photo background isn’t included.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        SectionTitle("About")
        LinkRow("Source code and releases", "github.com/${BuildConfig.UPDATE_REPO}") { uri.openUri(repo) }
        LinkRow("Font licences", "Open-source fonts from Google Fonts") { uri.openUri("$repo/blob/main/FONT_LICENSES.txt") }
        LinkRow(
            "Theme credits",
            "Catppuccin, Rosé Pine, Nord, Gruvbox, Dracula, Solarized, Tokyo Night, Everforest, Kanagawa, One Dark",
        ) { uri.openUri("$repo#credits") }
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
                "This backup has ${backup.summary}.\n\n" +
                    "Add to mine: adds them next to your quotes, skipping ones you already have.\n\n" +
                    "Replace everything: your quotes, collections and widget settings become the backup’s. " +
                    "You can undo this right after.",
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
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
